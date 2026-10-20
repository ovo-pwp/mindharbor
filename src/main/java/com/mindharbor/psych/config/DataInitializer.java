package com.mindharbor.psych.config;

import com.mindharbor.psych.entity.*;
import com.mindharbor.psych.enums.*;
import com.mindharbor.psych.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 首次启动时写入演示数据。已有用户数据则跳过。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final DoctorMapper doctorMapper;
    private final DoctorPatientMapper bindMapper;
    private final AssessmentMapper assessmentMapper;
    private final AssessmentQuestionMapper questionMapper;
    private final AssessmentRecordMapper recordMapper;
    private final ResourceMapper resourceMapper;
    private final ArticleMapper articleMapper;
    private final NoticeMapper noticeMapper;
    private final SymptomRecordMapper symptomMapper;
    private final ChatMessageMapper chatMapper;
    private final ReportMapper reportMapper;
    private final ActionLogMapper actionLogMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        Long exists = userMapper.selectCount(null);
        if (exists != null && exists > 0) {
            log.info("检测到已有数据，跳过初始化");
            return;
        }
        log.info("开始初始化演示数据…");
        String pwd = encoder.encode("123456");

        User admin = user("admin", pwd, "系统管理员", Role.ADMIN, null, null, null);
        User patient = user("zhangxiaoyu", pwd, "张小雨", Role.USER, "女", 26, "杭州");
        User d1 = user("liyisheng", pwd, "李明远", Role.DOCTOR, "男", 42, "杭州");
        User d2 = user("wangjing", pwd, "王静", Role.DOCTOR, "女", 36, "上海");
        User d3 = user("chenhao", pwd, "陈昊", Role.DOCTOR, "男", 45, "北京");
        User d4 = user("zhaowanru", pwd, "赵婉如", Role.DOCTOR, "女", 33, "深圳");
        User u1 = user("user01", pwd, "林一", Role.USER, "男", 22, "南京");
        User u2 = user("user02", pwd, "周舟", Role.USER, "女", 29, "成都");
        User u3 = user("user03", pwd, "何田", Role.USER, "男", 31, "武汉");
        User u4 = user("user04", pwd, "苏念", Role.USER, "女", 24, "广州");

        Doctor doc1 = doctor(d1, "杭州市第一人民医院", "副主任医师",
                "焦虑障碍,抑郁情绪,睡眠障碍", 8, "480.00", 4.9, 326,
                "精神科副主任医师，从事临床心理工作 8 年，擅长以 CBT 结合正念的方式处理焦虑与睡眠问题。相信「理解症状」比「消灭症状」更重要。");
        Doctor doc2 = doctor(d2, "上海心晴心理咨询中心", "国家二级心理咨询师",
                "亲密关系,原生家庭,情绪管理", 5, "360.00", 4.8, 218,
                "专注于成人依恋与亲密关系议题，累计个案时长 2000+ 小时。风格温和而直接，擅长陪伴来访者看见关系中的重复模式。");
        Doctor doc3 = doctor(d3, "北京师范大学心理学院", "临床心理学博士",
                "青少年厌学,考试焦虑,亲子沟通", 10, "600.00", 4.9, 412,
                "高校心理系教师，同时执业于青少年心理门诊。擅长与青少年及家庭工作，处理学业压力、厌学与亲子冲突。");
        Doctor doc4 = doctor(d4, "深圳南山心理健康中心", "注册心理师",
                "职场压力,职业倦怠,自我价值", 6, "420.00", 4.7, 189,
                "服务于多家企业的 EAP 项目，擅长职业倦怠、职场人际与自我价值感议题。练习正念八年，会把练习带进咨询。");

        List<Assessment> scales = seedAssessments();
        seedArticles();
        seedResources();
        seedNotices(admin);
        seedDemo(patient, doc1, d1, List.of(u1, u2, u3, u4), scales.get(0).getId(), scales.get(1).getId());

        log.info("初始化完成：账号 admin / zhangxiaoyu，密码均为 123456");
    }

    private User user(String username, String pwd, String name, Role role,
                      String gender, Integer age, String city) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(pwd);
        u.setRealName(name);
        u.setRole(role);
        u.setGender(gender);
        u.setAge(age);
        u.setCity(city);
        u.setStatus(CommonStatus.ACTIVE);
        u.setCreatedAt(LocalDateTime.now().minusDays(30));
        userMapper.insert(u);
        return u;
    }

    private Doctor doctor(User u, String hospital, String title, String specialty,
                          int years, String price, double rating, int consultCount, String intro) {
        Doctor d = new Doctor();
        d.setUserId(u.getId());
        d.setHospital(hospital);
        d.setTitle(title);
        d.setSpecialty(specialty);
        d.setIntro(intro);
        d.setYears(years);
        d.setPrice(new BigDecimal(price));
        d.setRating(rating);
        d.setConsultCount(consultCount);
        d.setCertNo("PSY-" + (1000 + u.getId()));
        d.setStatus(CommonStatus.APPROVED);
        d.setCreatedAt(LocalDateTime.now().minusDays(20));
        doctorMapper.insert(d);
        return d;
    }

    private List<Assessment> seedAssessments() {
        Assessment sds = assessment("SDS", "抑郁自评量表（SDS）",
                "由 Zung 编制的自评量表，共 20 题，用于评估最近一周的抑郁情绪程度。",
                "请根据最近一周的实际感受作答：1=没有或很少时间，2=小部分时间，3=相当多时间，4=绝大部分或全部时间。",
                "53,63,73", "正常,轻度抑郁,中度抑郁,重度抑郁");
        insertQuestions(sds.getId(), SeedData.SDS);

        Assessment sas = assessment("SAS", "焦虑自评量表（SAS）",
                "由 Zung 编制的自评量表，共 20 题，用于评估最近一周的焦虑主观感受。",
                "请根据最近一周的实际感受作答：1=没有或很少时间，2=小部分时间，3=相当多时间，4=绝大部分或全部时间。",
                "50,60,70", "正常,轻度焦虑,中度焦虑,重度焦虑");
        insertQuestions(sas.getId(), SeedData.SAS);
        return List.of(sds, sas);
    }

    private Assessment assessment(String code, String name, String desc, String instruction,
                                  String thresholds, String levels) {
        Assessment a = new Assessment();
        a.setCode(code);
        a.setName(name);
        a.setDescription(desc);
        a.setInstruction(instruction);
        a.setThresholds(thresholds);
        a.setLevels(levels);
        a.setEnabled(true);
        a.setQuestionCount(20);
        a.setCreatedAt(LocalDateTime.now().minusDays(30));
        assessmentMapper.insert(a);
        return a;
    }

    private void insertQuestions(Long assessmentId, String[][] data) {
        int seq = 1;
        for (String[] row : data) {
            AssessmentQuestion q = new AssessmentQuestion();
            q.setAssessmentId(assessmentId);
            q.setSeq(seq++);
            q.setContent(row[0]);
            q.setReverse("1".equals(row[1]));
            questionMapper.insert(q);
        }
    }

    private void seedArticles() {
        int i = 0;
        for (String[] row : SeedData.ARTICLES) {
            Article a = new Article();
            a.setTitle(row[0]);
            a.setCategory(row[1]);
            a.setSummary(row[2]);
            a.setContent(row[3]);
            a.setAuthor(row[4]);
            a.setViewCount(120 + i * 37);
            a.setStatus("PUBLISHED");
            a.setCreatedAt(LocalDateTime.now().minusDays(20 - i));
            articleMapper.insert(a);
            i++;
        }
    }

    private void seedResources() {
        int i = 0;
        for (String[] row : SeedData.RESOURCES) {
            Resource r = new Resource();
            r.setTitle(row[0]);
            r.setType(ResourceType.valueOf(row[1]));
            r.setCategory(row[2]);
            r.setContent(row[4]);
            r.setUploaderId(2L);
            r.setStatus(CommonStatus.APPROVED);
            r.setViewCount(60 + i * 23);
            r.setCreatedAt(LocalDateTime.now().minusDays(15 - i));
            resourceMapper.insert(r);
            i++;
        }
    }

    private void seedNotices(User admin) {
        for (String[] row : SeedData.NOTICES) {
            Notice n = new Notice();
            n.setTitle(row[0]);
            n.setContent(row[1]);
            n.setPublisherId(admin.getId());
            n.setStatus("PUBLISHED");
            n.setCreatedAt(LocalDateTime.now().minusDays(10));
            noticeMapper.insert(n);
        }
    }

    private void seedDemo(User patient, Doctor doc, User doctorUser, List<User> others,
                          Long sdsId, Long sasId) {
        Random random = new Random(42);

        // 近 14 天症状记录
        for (int i = 13; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            SymptomRecord r = new SymptomRecord();
            r.setUserId(patient.getId());
            int base = 3 - (13 - i) / 6;
            r.setMood(Math.max(1, Math.min(5, base + random.nextInt(2))));
            r.setSeverity(Math.max(1, Math.min(5, 4 - random.nextInt(2))));
            r.setEnergy(Math.max(1, Math.min(5, 2 + random.nextInt(2))));
            r.setTriggerEvent(i % 4 == 0 ? "项目上线前加班到 11 点" : (i % 5 == 0 ? "和室友因为家务争执" : null));
            r.setSymptoms(i % 3 == 0 ? "入睡困难,心慌" : "入睡困难");
            r.setCoping(i % 2 == 0 ? "散步,听音乐" : "刷手机");
            r.setNote(i % 3 == 0 ? "白天还好，晚上一个人待着就容易想很多。" : null);
            r.setRecordDate(date);
            r.setCreatedAt(date.atTime(22, 30));
            symptomMapper.insert(r);
        }

        // 测评记录
        saveRecord(patient.getId(), sdsId, "抑郁自评量表（SDS）", 46, 57.5, "轻度抑郁",
                "结果提示存在轻度情绪困扰。建议保持规律作息，每天 20 分钟中等强度运动，两周后复测。");
        saveRecord(patient.getId(), sasId, "焦虑自评量表（SAS）", 41, 51.3, "轻度焦虑",
                "结果提示存在轻度焦虑。建议尝试 4-7-8 呼吸法与情绪日记，若两周无改善建议预约咨询。");

        // 医患绑定
        DoctorPatient bp = new DoctorPatient();
        bp.setDoctorId(doc.getId());
        bp.setUserId(patient.getId());
        bp.setStatus(BindStatus.APPROVED);
        bp.setApplyReason("最近半年因为工作压力经常失眠，想找医生做几次咨询。");
        bp.setCreatedAt(LocalDateTime.now().minusDays(9));
        bp.setUpdatedAt(LocalDateTime.now().minusDays(8));
        bindMapper.insert(bp);

        // 聊天记录
        chat(doctorUser.getId(), patient.getId(),
                "你好，我看到你的记录了。最近睡眠大概是什么情况？", 8);
        chat(patient.getId(), doctorUser.getId(),
                "躺下以后要一两个小时才能睡着，半夜还会醒一两次。", 8);
        chat(doctorUser.getId(), patient.getId(),
                "听起来是入睡困难加上夜间易醒。白天会补觉吗？", 7);
        chat(patient.getId(), doctorUser.getId(),
                "周末会睡到中午，平时不太敢睡。", 7);
        chat(doctorUser.getId(), patient.getId(),
                "周末补觉太多会削弱晚上的睡意。这周我们可以先做一个实验：固定 7 点起床，白天不补觉超过 30 分钟。", 6);

        // 已发布报告
        Report report = new Report();
        report.setDoctorId(doc.getId());
        report.setUserId(patient.getId());
        report.setTitle("张小雨 · 阶段性心理评估报告");
        report.setOverview("本次评估覆盖来访者近 30 天的情绪与睡眠记录，共 14 条自评数据，以及 2 次量表测评"
                + "（SDS 标准分 57.5，SAS 标准分 51.3）。整体表现为轻度情绪困扰，主要问题集中在入睡困难与夜间易醒。");
        report.setInsight("情绪评分在最近两周呈缓慢下降趋势，与项目加班节点高度重合。触发事件集中在工作负荷与"
                + "室友关系两类。应对方式以「刷手机」为主，缺乏主动的生理调节手段，形成「越累越刷、越刷越晚」的循环。");
        report.setRiskWarning("目前未出现自伤或自杀相关表述，风险等级为低。需关注：若睡眠问题持续超过一个月，"
                + "或情绪评分连续一周低于 2 分，应升级干预频率。");
        report.setSuggestion("1. 固定 7:00 起床，白天补觉不超过 30 分钟；\n"
                + "2. 睡前 1 小时停止使用手机，改为纸质阅读或 4-7-8 呼吸练习；\n"
                + "3. 每周 3 次、每次 30 分钟的快走或慢跑；\n"
                + "4. 两周后复测 SDS 与 SAS，对比标准分变化。");
        report.setStatus("PUBLISHED");
        report.setCreatedAt(LocalDateTime.now().minusDays(3));
        report.setPublishedAt(LocalDateTime.now().minusDays(3));
        reportMapper.insert(report);

        // 行为日志：为协同过滤准备数据
        List<User> pool = new ArrayList<>(others);
        pool.add(patient);
        for (int u = 0; u < pool.size(); u++) {
            for (int res = 1; res <= 8; res++) {
                if ((u + res) % 3 == 0) {
                    continue;
                }
                ActionLog log = new ActionLog();
                log.setUserId(pool.get(u).getId());
                log.setTargetType("RESOURCE");
                log.setTargetId((long) res);
                log.setAction("VIEW");
                log.setWeight(1.0 + ((u + res) % 3) * 0.5);
                log.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(10)));
                actionLogMapper.insert(log);
            }
        }
    }

    private void saveRecord(Long userId, Long assessmentId, String name, int raw, double std, String level, String suggestion) {
        AssessmentRecord r = new AssessmentRecord();
        r.setUserId(userId);
        r.setAssessmentId(assessmentId);
        r.setAssessmentName(name);
        r.setRawScore(raw);
        r.setStandardScore(std);
        r.setLevel(level);
        r.setDetail("[]");
        r.setSuggestion(suggestion);
        r.setCreatedAt(LocalDateTime.now().minusDays(6));
        recordMapper.insert(r);
    }

    private void chat(Long from, Long to, String content, int daysAgo) {
        ChatMessage m = new ChatMessage();
        m.setFromId(from);
        m.setToId(to);
        m.setContent(content);
        m.setReadFlag(true);
        m.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        chatMapper.insert(m);
    }
}
