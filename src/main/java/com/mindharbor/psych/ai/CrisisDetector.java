package com.mindharbor.psych.ai;

import org.springframework.stereotype.Component;

import java.util.List;

/** 风险识别：出现自伤 / 自杀倾向时，回复后追加危机干预卡片 */
@Component
public class CrisisDetector {

    private static final List<String> HIGH_RISK = List.of(
            "自杀", "不想活", "活着没意思", "活着没有意思", "结束生命", "结束自己",
            "自残", "割腕", "跳楼", "死了算了", "想死", "轻生", "了结自己", "不想活了");

    private static final List<String> MEDIUM_RISK = List.of(
            "活着好累", "撑不下去", "没有意义", "绝望", "消失就好了", "没人在乎我");

    public boolean isHighRisk(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return HIGH_RISK.stream().anyMatch(text::contains);
    }

    public boolean isMediumRisk(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return MEDIUM_RISK.stream().anyMatch(text::contains);
    }

    public String riskLevel(String text) {
        if (isHighRisk(text)) {
            return "HIGH";
        }
        if (isMediumRisk(text)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /** 危机干预提示，随回复一起下发 */
    public String crisisCard() {
        return """

                ---

                > **我注意到你刚才的话里有一些很沉重的信号。你的感受很重要，也值得被认真对待。**
                >
                > 如果你此刻有伤害自己的想法，请立刻联系下面任意一个渠道，或者去最近的医院急诊：
                >
                > - **全国 24 小时心理援助热线：400-161-9995**
                > - **北京心理危机研究与干预中心：010-82951332**
                > - **希望 24 热线：400-161-9995（转 1）**
                > - **紧急情况请拨打 120 或 110**
                >
                > 我是一个 AI 助手，能陪你说话，但不能替代专业帮助。要不要试着联系一位真人咨询师？
                """;
    }
}
