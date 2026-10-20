package com.mindharbor.psych.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 本地共情回应引擎。
 * 未配置大模型 API Key 时接管对话，保证 AI 咨询模块始终可用。
 * 策略：先查知识库 → 未命中则情绪主题识别 → 共情反映 → 正常化 → 可执行方法 → 开放式提问。
 */
@Component
@RequiredArgsConstructor
public class FallbackResponder {

    private final KnowledgeBase knowledgeBase;

    private record Theme(String code, List<String> keywords, String empathy, String normalize,
                         List<String> methods, List<String> questions) {
    }

    private static final List<Theme> THEMES = List.of(
            new Theme("ANXIETY",
                    List.of("焦虑", "紧张", "担心", "害怕", "慌", "不安", "恐惧", "心慌", "发抖"),
                    "听起来你这段时间一直被一种「悬着」的感觉牵着走，身体和念头都很难真正放松下来。",
                    "焦虑本质上是身体在试图保护你——它把危险信号放大了。这不是你脆弱，而是警报系统太灵敏了。",
                    List.of("**4-7-8 呼吸**：吸气 4 秒、屏息 7 秒、呼气 8 秒，重复 4 轮。呼气比吸气长，能直接给神经系统踩刹车。",
                            "**把担心写下来**：拿一张纸分两列，左边写「我能控制的」，右边写「我控制不了的」。只对左边那一列做计划。",
                            "**5-4-3-2-1 落地法**：说出你看到的 5 样东西、摸到的 4 样、听到的 3 种声音、闻到的 2 种气味、尝到的 1 种味道，把注意力拉回当下。"),
                    List.of("这种紧张感最常在什么时候冒出来？是某个具体场景，还是几乎整天都在？",
                            "如果焦虑能说话，它最想提醒你注意什么？")),

            new Theme("DEPRESSION",
                    List.of("难过", "抑郁", "哭", "没意思", "没动力", "空虚", "提不起劲", "不想动",
                            "低落", "开心不起来", "吃不下", "没胃口", "不想吃", "食欲", "暴食", "睡不着觉"),
                    "听起来你这段时间整个人都在往下坠——不是某一件事过不去，是连吃饭、睡觉这些最基本的事都开始变得费劲。",
                    "情绪低落到一定程度时，动力系统和食欲会先关掉，这不是懒，也不是意志力问题。恢复的顺序是「先动起来，情绪才会跟上」，而不是反过来。",
                    List.of("**把目标缩到极小**：不要定「今天要学习三小时」，改成「把书翻开、读一页」，或者「先吃两口」。完成比完美重要。",
                            "**行为激活记录**：每天记一件做完的小事和当时 0-10 分的情绪值，一周后你会看到行动和情绪的真实关系。",
                            "**晒太阳 15 分钟**：上午的自然光照对情绪和食欲都有明确的调节作用，比在屋里待一天要好得多。"),
                    List.of("最近有没有哪一刻，哪怕很短，你觉得稍微轻松一点？那时候你在做什么？",
                            "这种状态大概持续多久了？")),

            new Theme("SLEEP",
                    List.of("失眠", "睡不着", "早醒", "多梦", "熬夜", "睡眠", "半夜醒"),
                    "睡不着的夜晚最难熬的地方，是白天还能分散的念头，到了夜里全回来了。",
                    "失眠往往是结果而不是原因——它常常是白天没处理完的情绪，在夜里排队找你。",
                    List.of("**刺激控制**：床只用来睡。躺 20 分钟没睡着就起来，到别的房间做点无聊的事，有困意再回床上。这条比任何助眠产品都有效。",
                            "**睡前 1 小时「关机仪式」**：固定动作（洗漱—关灯—听同一段白噪音），给大脑一个可预测的入睡信号。",
                            "**别补觉超过 30 分钟**：白天补太久会削弱晚上的睡意，容易越补越乱。"),
                    List.of("躺下之后，脑子里最常转的是哪一类事？",
                            "这种情况持续多久了？是最近才开始的，还是已经有一阵子？")),

            new Theme("PRESSURE",
                    List.of("压力", "加班", "考试", "考研", "考公", "工作", "老板", "内卷", "deadline", "任务", "干不完", "学习"),
                    "被任务追着跑的感觉，会让人连喘口气都觉得在浪费时间。",
                    "压力本身不是敌人，长期没有恢复期的压力才是。你现在缺的可能不是更强的意志，而是真正的停顿。",
                    List.of("**划分「必须」和「想要」**：今天只挑一件必须完成的，其余允许它不完美地待着。",
                            "**番茄钟 25+5**：25 分钟只做一件事，5 分钟必须离开座位。休息不是奖励，是维持输出的必要成本。",
                            "**给压力一个容器**：每天固定 15 分钟「专门担心时间」，其余时间冒出念头就记下来，留到那个时段再想。"),
                    List.of("现在压在你身上最重的那件事，具体是什么？",
                            "如果把这件事的难度从 10 分往下调，你希望先砍掉哪一部分？")),

            new Theme("RELATION",
                    List.of("分手", "吵架", "孤独", "没人", "朋友", "父母", "老公", "老婆", "男朋友", "女朋友", "关系", "被误解", "委屈"),
                    "关系里的伤，往往不是因为不在意，恰恰是因为太在意了。",
                    "被重要的人误解或疏远时，感到委屈和孤独是正常的反应，不需要为此感到羞耻。",
                    List.of("**区分事实和解读**：把「他根本不在乎我」换成「他今天没回我消息」。前一句是解读，后一句才是事实。",
                            "**用「我」开头表达**：把「你总是这样」换成「你这样做的时候，我感觉……」。指责会激起防御，感受才有可能被听见。",
                            "**先照顾自己的情绪**：在情绪峰值时沟通，几乎一定会升级冲突。给自己 20 分钟冷静期。"),
                    List.of("这件事里，最让你难受的是对方的行为，还是它让你对自己产生了怀疑？",
                            "如果可以重来一次，你最想对他说的一句话是什么？")),

            new Theme("ANGER",
                    List.of("生气", "愤怒", "气死", "烦躁", "暴躁", "火大", "想骂", "受不了"),
                    "那种火一下子上来的感觉，确实很难在当下压住。",
                    "愤怒通常是被侵犯了边界之后的信号——它在提醒你，有些东西对你很重要，而且被越过了。",
                    List.of("**生理降温先于讲道理**：用冷水洗把脸，或做 10 次深蹲。生理唤醒降下来，理性才回得来。",
                            "**写下边界**：具体是哪句话、哪个行为越界了？把它写清楚，比笼统地「他不尊重我」更有用。",
                            "**延迟回应**：告诉对方「我现在情绪很大，我们半小时后再谈」。这不是逃避，是负责。"),
                    List.of("如果给这股火打个分，0 到 10 分，现在是几分？",
                            "对方具体做了什么，让你觉得被冒犯了？")),

            new Theme("SOMATIC",
                    List.of("头疼", "胸闷", "喘不上气", "胃疼", "恶心", "心悸", "身体", "不舒服", "乏力", "没力气", "手抖"),
                    "身体先替你说了那些说不出口的话，这在心理学上很常见。",
                    "情绪压力会通过自主神经系统影响身体，检查不出器质性问题，不代表你的不舒服是「假的」。",
                    List.of("**记录发作场景**：什么时间、什么情境、持续多久。规律往往比症状本身更有信息量。",
                            "**渐进式肌肉放松**：从脚趾开始，绷紧 5 秒再放松 10 秒，一路做到面部。",
                            "**先排除器质性问题**：建议做一次基础体检，把身体因素排除掉，再回到情绪层面看。"),
                    List.of("这些身体反应，通常在什么情境下出现？",
                            "最近一次发作是什么时候，当时你正在做什么？")),

            new Theme("SELF_WORTH",
                    List.of("自卑", "没用", "不够好", "失败", "配不上", "讨厌自己", "自责", "否定自己"),
                    "你对自己说的那些话，可能比任何人对你说的都要严厉。",
                    "我们对自己的评价，常常来自很早以前别人对我们的评价。它听起来像事实，其实只是一个被重复了很多遍的观点。",
                    List.of("**三栏记录法**：情境 / 自动冒出的念头 / 反驳它的证据。坚持一周，你会发现念头和事实之间的缝隙。",
                            "**用对朋友的方式对自己**：如果是好朋友说「我真没用」，你会怎么回他？把那句话原样说给自己听。",
                            "**记录被肯定的事**：每天写一件「今天我做到了」，哪怕只是按时吃了饭。"),
                    List.of("这个「我不够好」的声音，最早是从什么时候开始出现的？",
                            "如果有个朋友和你经历一样的事，你会觉得他没用吗？")),

            new Theme("GENERAL",
                    List.of(),
                    "谢谢你愿意把这些说出来。愿意开口本身，就已经是在照顾自己了。",
                    "情绪没有对错，它只是在告诉你，有些需要没有被满足。",
                    List.of("**给情绪命名**：试着用更准确的词描述现在的感受（烦躁？失落？委屈？无力？）。命名本身就能降低强度。",
                            "**身体先稳住**：喝口水、站起来走两分钟、把肩膀放松下来。情绪调节很多时候是从身体开始的。",
                            "**找一个安全的出口**：写下来、录一段语音给自己、或找一个不会评判你的人说说。"),
                    List.of("如果用一个词形容你现在最核心的感受，会是什么？",
                            "这件事对你的影响，最明显的是在情绪、睡眠，还是和别人的关系上？"))
    );

    private static final String DISCLAIMER = """
            > 我是「心屿」AI 心理助手，会陪你梳理情绪、提供一些可尝试的方法。
            > **我不能做诊断，也不能替代专业咨询。** 如果困扰持续两周以上，或影响到吃饭、睡觉、工作，建议尽快联系一位真人咨询师。
            """;

    private static final String[] OPENERS = {
            "我在听。",
            "嗯，我听到你了。",
            "我在这儿，慢慢说。",
            "好，我们一点一点来。"
    };

    /**
     * 生成回复。
     * 优先走知识库（能回答「什么是 CBT」这类问题），未命中再走情绪共情模板。
     *
     * @param userText  用户最后一条消息
     * @param firstTurn 是否本轮对话的第一条回复
     */
    public String reply(String userText, boolean firstTurn) {
        String knowledge = knowledgeBase.answer(userText);
        if (knowledge != null) {
            StringBuilder sb = new StringBuilder();
            if (firstTurn) {
                sb.append(DISCLAIMER).append("\n");
            }
            sb.append(knowledge);
            sb.append("\n\n*（本地知识库模式 · 配置 `DASHSCOPE_API_KEY` 可开启大模型自由对话）*");
            return sb.toString();
        }

        Theme theme = match(userText);
        StringBuilder sb = new StringBuilder();

        if (firstTurn) {
            sb.append(DISCLAIMER).append("\n");
        }
        sb.append(pick(OPENERS)).append("\n\n");
        sb.append(theme.empathy()).append("\n\n");
        sb.append(theme.normalize()).append("\n\n");
        sb.append("**也许可以试试：**\n\n");
        for (String method : theme.methods()) {
            sb.append("- ").append(method).append("\n");
        }
        sb.append("\n").append(pick(theme.questions())).append("\n");
        sb.append("\n*（本地共情模式 · 配置 `DASHSCOPE_API_KEY` 可开启大模型自由对话）*");
        return sb.toString();
    }

    private Theme match(String text) {
        if (text == null || text.isBlank()) {
            return THEMES.get(THEMES.size() - 1);
        }
        Theme best = THEMES.get(THEMES.size() - 1);
        int bestHits = 0;
        for (Theme t : THEMES) {
            int hits = 0;
            for (String k : t.keywords()) {
                if (text.contains(k)) {
                    hits++;
                }
            }
            if (hits > bestHits) {
                bestHits = hits;
                best = t;
            }
        }
        return best;
    }

    private String pick(List<String> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private String pick(String[] arr) {
        return arr[ThreadLocalRandom.current().nextInt(arr.length)];
    }

    /** 生成会话标题：取首句前 18 个字 */
    public String title(String firstMessage) {
        if (firstMessage == null || firstMessage.isBlank()) {
            return "新的对话";
        }
        String clean = firstMessage.replaceAll("[\\r\\n]+", " ").trim();
        return clean.length() <= 18 ? clean : clean.substring(0, 18) + "…";
    }
}
