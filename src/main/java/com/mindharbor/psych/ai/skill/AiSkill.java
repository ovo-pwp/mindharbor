package com.mindharbor.psych.ai.skill;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * AI 可调用的一项「技能」。
 *
 * @param name        函数名（snake_case），大模型 function calling 时使用
 * @param description 给模型看的用途说明，写清楚「什么时候该用」
 * @param parameters  JSON Schema 参数定义
 * @param triggers    本地模式（无 API Key）下的关键词触发词
 * @param handler     实际执行逻辑：(userId, args) -> 给模型/用户看的结果文本
 */
public record AiSkill(
        String name,
        String description,
        Map<String, Object> parameters,
        List<String> triggers,
        BiFunction<Long, Map<String, Object>, String> handler
) {
}
