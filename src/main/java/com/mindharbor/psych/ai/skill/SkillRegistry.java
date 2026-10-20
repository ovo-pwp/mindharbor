package com.mindharbor.psych.ai.skill;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mindharbor.psych.entity.*;
import com.mindharbor.psych.mapper.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 技能注册中心。
 *
 * 两条使用路径：
 * 1. 有大模型 Key：把 {@link #toolsPayload()} 作为 tools 传给模型，由模型决定调哪个、传什么参数
 * 2. 无 Key：用 {@link #matchLocal(String)} 按关键词+参数抽取做规则触发，保证离线也能演示
 *
 * 所有技能都带 userId 隔离，只能操作当前登录用户自己的数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillRegistry {

    private final SymptomRecordMapper symptomMapper;
    private final AssessmentRecordMapper assessmentMapper;
    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;

    private final Map<String, AiSkill> skills = new LinkedHashMap<>();

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @PostConstruct
    void init() {
        register(recordMood());
        register(queryMood());
        register(myAssessments());
        register(recommendCounselor());
        register(searchArticle());
        register(crisisResources());
    }

    private void register(AiSkill skill) {
        skills.put(skill.name(), skill);
    }

    // ==================== 给大模型用的 tools 定义 ====================

    public List<Map<String, Object>> toolsPayload() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AiSkill s : skills.values()) {
            list.add(Map.of(
                    "type", "function",
                    "function", Map.of(
                            "name", s.name(),
                            "description", s.description(),
                            "parameters", s.parameters()
                    )));
        }
        return list;
    }

    public String execute(Long userId, String name, Map<String, Object> args) {
        AiSkill skill = skills.get(name);
        if (skill == null) {
            return "没有名为 " + name + " 的技能。";
        }
        try {
            String result = skill.handler().apply(userId, args == null ? Map.of() : args);
            log.debug("skill {} executed for user {}: {}", name, userId, result);
            return result;
        } catch (Exception e) {
            log.warn("skill {} 执行失败", name, e);
            return "执行 " + name + " 时出错：" + e.getMessage();
        }
    }

    /** 本地模式：判断这条消息是否触发某个技能，返回命中的技能名（无则 null） */
    public AiSkill matchLocal(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        // 1. 强触发词：直接命中就用
        for (AiSkill s : skills.values()) {
            for (String t : s.triggers()) {
                if (text.contains(t)) {
                    return s;
                }
            }
        }
        // 2. 组合判断：对象词 + 意图词。光靠固定短语会漏掉「有没有适合焦虑的咨询师」这类说法
        if (containsAny(text, "咨询师", "心理医生", "医生", "老师")
                && containsAny(text, "有没有", "推荐", "哪个", "哪位", "适合", "找", "约", "帮我")) {
            return skills.get("recommend_counselor");
        }
        if (containsAny(text, "文章", "科普", "资料", "书")
                && containsAny(text, "有没有", "推荐", "想看", "读", "了解", "找", "几篇")) {
            return skills.get("search_article");
        }
        return null;
    }

    /** 本地模式：从自然语言里粗抽参数 */
    public Map<String, Object> extractLocalArgs(String skillName, String text) {
        Map<String, Object> args = new HashMap<>();
        switch (skillName) {
            case "record_mood" -> {
                args.put("mood", guessMood(text));
                args.put("note", text);
            }
            case "query_mood" -> args.put("days", 7);
            case "recommend_counselor" -> args.put("issue", text);
            case "search_article" -> args.put("keyword", guessKeyword(text));
            default -> {
            }
        }
        return args;
    }

    private int guessMood(String text) {
        if (containsAny(text, "很差", "糟透", "崩溃", "绝望", "难受死了")) return 1;
        if (containsAny(text, "不好", "低落", "难过", "烦", "郁闷", "丧")) return 2;
        if (containsAny(text, "一般", "还行", "凑合", "平稳")) return 3;
        if (containsAny(text, "不错", "挺好", "开心", "轻松", "还行吧")) return 4;
        if (containsAny(text, "很好", "特别好", "超开心", "棒")) return 5;
        return 3;
    }

    private String guessKeyword(String text) {
        String cleaned = text.replaceAll(
                        "(帮我|给我|找一下|搜一下|有没有|推荐|想看|看看|了解一下|了解|关于|相关|几篇|一些|的|科普|文章|资料|找|搜)",
                        "")
                .replaceAll("^[，。、！？：\\s]+|[，。、！？：\\s]+$", "")
                .trim();
        return cleaned.length() > 20 ? cleaned.substring(0, 20) : cleaned;
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    /** 议题词表：用户说的是「焦虑」，医生擅长写的是「焦虑障碍」，用共同主题词做匹配 */
    private static final List<String> TOPICS = List.of(
            "焦虑", "抑郁", "睡眠", "失眠", "亲密", "关系", "家庭", "原生", "职场", "压力",
            "青少年", "考试", "厌学", "亲子", "情绪", "自我", "职业", "倦怠");

    /**
     * 医生擅长领域与用户议题是否命中同一个主题词。
     * 不能用「整句包含擅长项」，否则「有没有适合焦虑的咨询师」匹配不到「焦虑障碍」。
     */
    private boolean hitsTopic(String specialty, String issue) {
        if (issue == null || issue.isBlank()) {
            return false;
        }
        for (String s : specialty.split("[,，、]+")) {
            String item = s.trim();
            for (String t : TOPICS) {
                if (issue.contains(t) && item.contains(t)) {
                    return true;
                }
            }
            if (issue.contains(item)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 具体技能 ====================

    private AiSkill recordMood() {
        Map<String, Object> params = Map.of(
                "type", "object",
                "properties", Map.of(
                        "mood", Map.of("type", "integer", "minimum", 1, "maximum", 5,
                                "description", "整体情绪评分，1 很差，5 很好"),
                        "severity", Map.of("type", "integer", "minimum", 1, "maximum", 5,
                                "description", "困扰程度，1 很轻，5 很重，默认 3"),
                        "energy", Map.of("type", "integer", "minimum", 1, "maximum", 5,
                                "description", "精力水平，1 很低，5 充沛，默认 3"),
                        "trigger_event", Map.of("type", "string", "description", "触发事件，一句话"),
                        "note", Map.of("type", "string", "description", "用户原话或补充说明")
                ),
                "required", List.of("mood")
        );
        return new AiSkill("record_mood",
                "帮用户记录今天的情绪状态。当用户描述自己此刻或今天的感受（如「今天心情很差」「我最近很焦虑」）并希望留下记录时调用。",
                params,
                List.of("记录一下", "帮我记", "记一下", "记下来", "记录情绪"),
                (userId, args) -> {
                    int mood = clamp(asInt(args, "mood", 3), 1, 5);
                    int severity = clamp(asInt(args, "severity", 6 - mood), 1, 5);
                    int energy = clamp(asInt(args, "energy", 3), 1, 5);
                    SymptomRecord r = new SymptomRecord();
                    r.setUserId(userId);
                    r.setMood(mood);
                    r.setSeverity(severity);
                    r.setEnergy(energy);
                    r.setTriggerEvent(str(args, "trigger_event"));
                    r.setNote(str(args, "note"));
                    r.setSymptoms("");
                    r.setCoping("");
                    r.setRecordDate(LocalDate.now());
                    r.setCreatedAt(LocalDateTime.now());
                    symptomMapper.insert(r);

                    Long today = symptomMapper.selectCount(Wrappers.<SymptomRecord>lambdaQuery()
                            .eq(SymptomRecord::getUserId, userId)
                            .eq(SymptomRecord::getRecordDate, LocalDate.now()));
                    return "已记录今天的情绪：情绪 %d/5，困扰 %d/5，精力 %d/5%s。今天共 %d 条记录，在「情绪记录」里能看到趋势图。"
                            .formatted(mood, severity, energy,
                                    r.getTriggerEvent() == null || r.getTriggerEvent().isBlank()
                                            ? "" : "，触发事件「" + r.getTriggerEvent() + "」",
                                    today == null ? 1 : today);
                });
    }

    private AiSkill queryMood() {
        Map<String, Object> params = Map.of(
                "type", "object",
                "properties", Map.of(
                        "days", Map.of("type", "integer", "minimum", 1, "maximum", 90,
                                "description", "统计最近多少天，默认 7")
                )
        );
        return new AiSkill("query_mood",
                "查询用户最近的情绪记录并给出趋势小结。当用户问「我最近情绪怎么样」「我这周状态如何」时调用。",
                params,
                List.of("最近情绪", "情绪怎么样", "状态怎么样", "我记录", "这周状态"),
                (userId, args) -> {
                    int days = clamp(asInt(args, "days", 7), 1, 90);
                    LocalDate from = LocalDate.now().minusDays(days);
                    List<SymptomRecord> list = symptomMapper.selectList(Wrappers.<SymptomRecord>lambdaQuery()
                            .eq(SymptomRecord::getUserId, userId)
                            .ge(SymptomRecord::getRecordDate, from)
                            .orderByAsc(SymptomRecord::getRecordDate));
                    if (list.isEmpty()) {
                        return "最近 %d 天还没有情绪记录。要不要现在记一条？".formatted(days);
                    }
                    double avgMood = list.stream().mapToInt(SymptomRecord::getMood).average().orElse(0);
                    double avgSev = list.stream().mapToInt(SymptomRecord::getSeverity).average().orElse(0);
                    SymptomRecord low = list.stream().min(Comparator.comparingInt(SymptomRecord::getMood)).orElse(list.get(0));
                    String trigger = list.stream().map(SymptomRecord::getTriggerEvent)
                            .filter(s -> s != null && !s.isBlank()).distinct().limit(3)
                            .collect(Collectors.joining("、"));
                    String trend = list.size() >= 4
                            ? (list.get(list.size() - 1).getMood() >= list.get(0).getMood() ? "整体在往上走" : "整体偏低，需要留意")
                            : "记录还比较少，趋势参考意义有限";
                    return "最近 %d 天共 %d 条记录：平均情绪 %.1f/5，平均困扰 %.1f/5，%s。最低的一天是 %s（情绪 %d）。%s"
                            .formatted(days, list.size(), avgMood, avgSev, trend,
                                    low.getRecordDate(), low.getMood(),
                                    trigger.isBlank() ? "" : "高频触发事件：" + trigger + "。");
                });
    }

    private AiSkill myAssessments() {
        Map<String, Object> params = Map.of("type", "object", "properties", Map.of());
        return new AiSkill("my_assessments",
                "查询用户最近做过的心理测评结果（SDS/SAS 分数与分级）。当用户问「我上次测评多少分」「我的测评结果」时调用。",
                params,
                List.of("测评结果", "我测过", "上次测评", "我的测评"),
                (userId, args) -> {
                    List<AssessmentRecord> list = assessmentMapper.selectList(
                            Wrappers.<AssessmentRecord>lambdaQuery()
                                    .eq(AssessmentRecord::getUserId, userId)
                                    .orderByDesc(AssessmentRecord::getCreatedAt)
                                    .last("limit 3"));
                    if (list.isEmpty()) {
                        return "你还没有做过测评。平台有 SDS（抑郁自评）和 SAS（焦虑自评）两份量表，各 20 题，大概 3 分钟。";
                    }
                    StringBuilder sb = new StringBuilder("最近的测评记录：\n");
                    for (AssessmentRecord r : list) {
                        sb.append("- %s：标准分 %s，结论「%s」，%s\n".formatted(
                                r.getAssessmentName(), fmtScore(r.getStandardScore()),
                                r.getLevel(), r.getCreatedAt().format(DT)));
                    }
                    sb.append("\n量表只是筛查，不构成诊断。");
                    return sb.toString();
                });
    }

    private AiSkill recommendCounselor() {
        Map<String, Object> params = Map.of(
                "type", "object",
                "properties", Map.of(
                        "issue", Map.of("type", "string", "description", "用户想处理的议题，如焦虑、失眠、亲密关系")
                ),
                "required", List.of("issue")
        );
        return new AiSkill("recommend_counselor",
                "根据用户想处理的议题推荐平台上的咨询师。当用户问「有没有适合我的咨询师」「谁能帮我解决失眠」时调用。",
                params,
                List.of("推荐咨询师", "推荐医生", "找咨询师", "哪个咨询师", "适合我的医生"),
                (userId, args) -> {
                    String issue = str(args, "issue");
                    List<Doctor> all = doctorMapper.selectList(Wrappers.<Doctor>lambdaQuery()
                            .eq(Doctor::getStatus, com.mindharbor.psych.enums.CommonStatus.APPROVED)
                            .orderByDesc(Doctor::getRating));
                    if (all.isEmpty()) {
                        return "目前还没有入驻的咨询师。";
                    }
                    List<Doctor> matched = all.stream()
                            .filter(d -> d.getSpecialty() != null && hitsTopic(d.getSpecialty(), issue))
                            .limit(3).toList();
                    List<Doctor> target = matched.isEmpty() ? all.stream().limit(3).toList() : matched;
                    StringBuilder sb = new StringBuilder(matched.isEmpty()
                            ? "没有完全对口的，这几位评价最高，可以先聊聊看：\n"
                            : "按你提到的情况，这几位比较对口：\n");
                    for (Doctor d : target) {
                        User u = userMapper.selectById(d.getUserId());
                        sb.append("- **%s**（%s，%s）擅长 %s，¥%s/次，评分 %s，已接 %d 例\n".formatted(
                                u == null ? "咨询师" : u.getRealName(), d.getTitle(), d.getHospital(),
                                d.getSpecialty(), d.getPrice(), d.getRating(), d.getConsultCount()));
                    }
                    sb.append("\n可以在「找咨询师」里查看详情并申请。");
                    return sb.toString();
                });
    }

    private AiSkill searchArticle() {
        Map<String, Object> params = Map.of(
                "type", "object",
                "properties", Map.of(
                        "keyword", Map.of("type", "string", "description", "搜索关键词")
                ),
                "required", List.of("keyword")
        );
        return new AiSkill("search_article",
                "在平台的心理科普文章里检索。当用户想深入了解某个概念、想要阅读材料时调用。",
                params,
                List.of("科普", "有没有文章", "相关文章", "想了解", "读一下"),
                (userId, args) -> {
                    String raw = str(args, "keyword");
                    final String kw = (raw == null || raw.isBlank()) ? "" : raw;
                    List<Article> list = articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                            .eq(Article::getStatus, "PUBLISHED")
                            .and(w -> w.like(Article::getTitle, kw).or().like(Article::getCategory, kw)
                                    .or().like(Article::getSummary, kw))
                            .orderByDesc(Article::getViewCount)
                            .last("limit 3"));
                    if (list.isEmpty()) {
                        return "没找到「" + kw + "」相关的科普文章。可以换个说法，或直接问我也行。";
                    }
                    StringBuilder sb = new StringBuilder("找到这几篇：\n");
                    for (Article a : list) {
                        sb.append("- **%s**（%s）— %s\n".formatted(a.getTitle(), a.getCategory(), a.getSummary()));
                    }
                    sb.append("\n在「心理科普」里可以打开全文。");
                    return sb.toString();
                });
    }

    private AiSkill crisisResources() {
        Map<String, Object> params = Map.of("type", "object", "properties", Map.of());
        return new AiSkill("crisis_resources",
                "当用户流露出自伤、自杀倾向，或询问危机求助渠道时调用，返回即时可用的求助资源。",
                params,
                List.of("自杀", "不想活", "活不下去", "想死", "危机热线"),
                (userId, args) -> """
                        如果你此刻有伤害自己的想法，请立刻联系下面任意一个渠道，或去最近的医院急诊：

                        - **全国 24 小时心理援助热线：400-161-9995**
                        - **北京心理危机研究与干预中心：010-82951332**
                        - **紧急情况：120 / 110**

                        你不是麻烦，你现在需要的是有人陪着你。\s""");
    }

    // ==================== 工具方法 ====================

    private int asInt(Map<String, Object> args, String key, int def) {
        Object v = args.get(key);
        if (v == null) {
            return def;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private String fmtScore(Double d) {
        if (d == null) {
            return "-";
        }
        return d == Math.floor(d) ? String.valueOf(d.intValue()) : String.valueOf(d);
    }

    public int count() {
        return skills.size();
    }

    public List<String> names() {
        return new ArrayList<>(skills.keySet());
    }
}
