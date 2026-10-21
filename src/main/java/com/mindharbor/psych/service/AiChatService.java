package com.mindharbor.psych.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindharbor.psych.ai.CrisisDetector;
import com.mindharbor.psych.ai.FallbackResponder;
import com.mindharbor.psych.ai.QwenClient;
import com.mindharbor.psych.ai.skill.AiSkill;
import com.mindharbor.psych.ai.skill.SkillRegistry;
import com.mindharbor.psych.common.BizException;
import com.mindharbor.psych.dto.ChatDTO;
import com.mindharbor.psych.entity.AiMessage;
import com.mindharbor.psych.entity.AiSession;
import com.mindharbor.psych.mapper.AiMessageMapper;
import com.mindharbor.psych.mapper.AiSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;
    private final QwenClient qwenClient;
    private final FallbackResponder fallbackResponder;
    private final CrisisDetector crisisDetector;
    private final SkillRegistry skillRegistry;
    private final ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /** 单轮对话里最多允许模型连续调用工具的轮数，防止死循环 */
    private static final int MAX_TOOL_ROUNDS = 3;

    private static final String SYSTEM_PROMPT = """
            你是「心屿」AI 心理助手，一位受过专业训练的心理支持对话者。你的任务是陪伴、倾听、帮助来访者梳理情绪。

            【必须遵守】
            1. 只做情绪支持与心理健康教育，**绝不进行医学诊断、不开具药物建议、不替代专业治疗**。
            2. 回复结构：先共情（准确反映对方的感受）→ 再正常化（说明这是常见反应）→ 再给 1-2 个具体可执行的方法 → 最后用一个开放式问题把话头交回对方。
            3. 语言温和、具体、不说教。避免「你应该」「你必须」，多用「也许可以试试」「有没有可能」。
            4. 单次回复 200-400 字，可用少量 Markdown（小标题、列表），不要长篇大论。
            5. 如果对方提到自伤、自杀、伤害他人，**立即**表达关心与陪伴，明确告知全国心理援助热线 400-161-9995，并鼓励其联系身边信任的人或前往医院急诊。不要说教，不要恐慌。
            6. 不做价值判断，不追问隐私细节，对方不愿说的不勉强。
            7. 不要暴露你是「规则驱动」的，也不要反复声明自己是 AI，只在必要时说明局限。

            【工具使用】
            你可以调用平台提供的技能（工具）。判断标准：
            - 用户希望**记录**今天的感受 → 调用 record_mood
            - 用户问「我最近状态怎么样」→ 调用 query_mood
            - 用户问测评结果 → 调用 my_assessments
            - 用户想找咨询师 → 调用 recommend_counselor
            - 用户想深入了解某个概念/想读材料 → 调用 search_article
            - 用户流露自伤自杀倾向 → 调用 crisis_resources
            工具返回的是事实数据，你需要用自然、有温度的语言转述，不要把原始结果直接念出来。
            不需要工具时就直接对话，不要为了用工具而用工具。
            """;

    private static final Map<String, String> SKILL_LABELS = Map.of(
            "record_mood", "记录情绪",
            "query_mood", "查看情绪趋势",
            "my_assessments", "查询测评记录",
            "recommend_counselor", "推荐咨询师",
            "search_article", "检索科普文章",
            "crisis_resources", "危机求助资源"
    );

    public List<AiSession> sessions(Long userId) {
        return sessionMapper.selectList(Wrappers.<AiSession>lambdaQuery()
                .eq(AiSession::getUserId, userId)
                .orderByDesc(AiSession::getUpdatedAt));
    }

    public AiSession createSession(Long userId, String title) {
        AiSession s = new AiSession();
        s.setUserId(userId);
        s.setTitle(title == null || title.isBlank() ? "新的对话" : title);
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(s);
        return s;
    }

    public List<AiMessage> messages(Long userId, Long sessionId) {
        AiSession s = requireSession(userId, sessionId);
        return messageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getSessionId, s.getId())
                .orderByAsc(AiMessage::getId));
    }

    public void deleteSession(Long userId, Long sessionId) {
        requireSession(userId, sessionId);
        sessionMapper.deleteById(sessionId);
        messageMapper.delete(Wrappers.<AiMessage>lambdaQuery().eq(AiMessage::getSessionId, sessionId));
    }

    private AiSession requireSession(Long userId, Long sessionId) {
        AiSession s = sessionMapper.selectById(sessionId);
        if (s == null || !s.getUserId().equals(userId)) {
            throw new BizException(404, "会话不存在");
        }
        return s;
    }

    /** 流式对话：SSE 推流 */
    public SseEmitter chat(Long userId, ChatDTO dto) {
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BizException("请输入内容");
        }
        String content = dto.getContent().trim();
        if (content.length() > 2000) {
            throw new BizException("单条消息不要超过 2000 字");
        }

        AiSession session;
        boolean newSession = dto.getSessionId() == null;
        if (newSession) {
            session = createSession(userId, fallbackResponder.title(content));
        } else {
            session = requireSession(userId, dto.getSessionId());
        }

        AiMessage userMsg = new AiMessage();
        userMsg.setSessionId(session.getId());
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        List<AiMessage> history = messageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getSessionId, session.getId())
                .orderByAsc(AiMessage::getId));

        SseEmitter emitter = new SseEmitter(180_000L);
        Long sessionId = session.getId();
        boolean firstTurn = history.stream().filter(m -> "assistant".equals(m.getRole())).findAny().isEmpty();

        executor.execute(() -> {
            StringBuilder answer = new StringBuilder();
            try {
                emitter.send(SseEmitter.event().data(event("meta", Map.of("sessionId", sessionId))));

                if (qwenClient.available()) {
                    runWithTools(userId, content, history, answer, emitter);
                } else {
                    runLocal(userId, content, firstTurn, answer, emitter);
                }

                // 本地模式已在 runLocal 里处理危机卡片；大模型模式再兜一层
                if (qwenClient.available() && crisisDetector.isHighRisk(content)) {
                    String card = crisisDetector.crisisCard();
                    answer.append(card);
                    streamOut(emitter, card);
                }

                if (answer.length() == 0) {
                    String fb = fallbackResponder.reply(content, firstTurn);
                    answer.append(fb);
                    streamOut(emitter, fb);
                }

                AiMessage aiMsg = new AiMessage();
                aiMsg.setSessionId(sessionId);
                aiMsg.setRole("assistant");
                aiMsg.setContent(answer.toString());
                aiMsg.setCreatedAt(LocalDateTime.now());
                messageMapper.insert(aiMsg);

                AiSession update = new AiSession();
                update.setId(sessionId);
                update.setUpdatedAt(LocalDateTime.now());
                sessionMapper.updateById(update);

                emitter.send(SseEmitter.event().data(event("done", Map.of(
                        "sessionId", sessionId,
                        "messageId", aiMsg.getId(),
                        "risk", crisisDetector.riskLevel(content)))));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 对话异常", e);
                try {
                    emitter.send(SseEmitter.event().data(event("error", Map.of("message", String.valueOf(e.getMessage())))));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * 大模型模式：带 tools 的多轮循环。
     * 模型返回 tool_calls → 执行技能 → 把结果作为 tool 消息回灌 → 再请求，直到模型直接作答。
     */
    private void runWithTools(Long userId, String content, List<AiMessage> history,
                              StringBuilder answer, SseEmitter emitter) {
        List<Map<String, Object>> payload = new ArrayList<>();
        payload.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        int from = Math.max(0, history.size() - 20);
        for (AiMessage m : history.subList(from, history.size())) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("role", m.getRole());
            msg.put("content", m.getContent() == null ? "" : m.getContent());
            payload.add(msg);
        }

        List<Map<String, Object>> tools = skillRegistry.toolsPayload();

        boolean anyToolCalled = false;

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            QwenClient.StreamOutcome out = qwenClient.stream(payload, tools,
                    delta -> {
                        answer.append(delta);
                        sendQuietly(emitter, event("delta", Map.of("content", delta)));
                    },
                    err -> sendQuietly(emitter, event("error", Map.of("message", err))));

            if (out.toolCalls().isEmpty()) {
                break;
            }
            anyToolCalled = true;

            // 回传 assistant 的 tool_calls，接着逐个执行
            Map<String, Object> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", out.content() == null || out.content().isBlank() ? null : out.content());
            List<Map<String, Object>> calls = new ArrayList<>();
            for (QwenClient.ToolCall tc : out.toolCalls()) {
                calls.add(Map.of("id", tc.id(), "type", "function",
                        "function", Map.of("name", tc.name(), "arguments", tc.arguments())));
            }
            assistantMsg.put("tool_calls", calls);
            payload.add(assistantMsg);

            for (QwenClient.ToolCall tc : out.toolCalls()) {
                sendQuietly(emitter, event("tool", Map.of(
                        "name", tc.name(),
                        "label", SKILL_LABELS.getOrDefault(tc.name(), tc.name()))));
                Map<String, Object> args = parseArgs(tc.arguments());
                String result = skillRegistry.execute(userId, tc.name(), args);
                Map<String, Object> toolMsg = new HashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", tc.id());
                toolMsg.put("content", result);
                payload.add(toolMsg);
            }
        }
        if (!anyToolCalled) {
            localFallback(userId, content, emitter);
        } else {
            log.debug("模型已调用工具，跳过本地兜底");
        }
    }

    /**
     * 本地兜底：模型没有（或不能）调用工具时，用关键词规则把操作做掉。
     *
     * 为什么需要：agnes-2.5-flash 这类模型不支持 function calling，
     * 用户说「帮我记录一下心情很差」它只会追问，数据库里什么都没有。
     * 这里让「说话」和「办事」解耦——模型负责温度，规则负责落地。
     */
    private void localFallback(Long userId, String content, SseEmitter emitter) {
        AiSkill skill = skillRegistry.matchLocal(content);
        if (skill == null) {
            return;
        }
        String label = SKILL_LABELS.getOrDefault(skill.name(), skill.name());
        sendQuietly(emitter, event("tool", Map.of("name", skill.name(), "label", label)));
        try {
            String result = skillRegistry.execute(userId, skill.name(),
                    skillRegistry.extractLocalArgs(skill.name(), content));
            log.info("本地兜底执行技能 {}：{}", skill.name(), result);
            sendQuietly(emitter, event("tool_result", Map.of("label", label, "text", result)));
        } catch (Exception e) {
            log.warn("本地兜底执行失败", e);
        }
    }

    /** 本地模式：技能规则触发 → 知识库 → 共情模板 */
    private void runLocal(Long userId, String content, boolean firstTurn,
                          StringBuilder answer, SseEmitter emitter) {
        AiSkill skill = skillRegistry.matchLocal(content);
        String text;
        if (skill != null) {
            String label = SKILL_LABELS.getOrDefault(skill.name(), skill.name());
            sendQuietly(emitter, event("tool", Map.of("name", skill.name(), "label", label)));
            String result = skillRegistry.execute(userId, skill.name(),
                    skillRegistry.extractLocalArgs(skill.name(), content));
            text = "**已为你执行「" + label + "」**\n\n" + result
                    + "\n\n*（本地技能模式 · 配置 `DASHSCOPE_API_KEY` 后由大模型自主判断调用）*";
        } else {
            text = fallbackResponder.reply(content, firstTurn);
        }
        answer.append(text);
        streamOut(emitter, text);

        if (crisisDetector.isHighRisk(content) && !text.contains("400-161-9995")) {
            String card = crisisDetector.crisisCard();
            answer.append(card);
            streamOut(emitter, card);
        }
    }

    private Map<String, Object> parseArgs(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("工具参数解析失败: {}", json);
            return Map.of();
        }
    }

    private String event(String type, Map<String, Object> payload) {
        Map<String, Object> map = new HashMap<>(payload);
        map.put("type", type);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"type\":\"error\"}";
        }
    }

    /** 按固定长度分块推送，模拟流式打字效果 */
    private void streamOut(SseEmitter emitter, String text) {
        for (int i = 0; i < text.length(); i += 12) {
            sendQuietly(emitter, event("delta",
                    Map.of("content", text.substring(i, Math.min(text.length(), i + 12)))));
            sleep(14);
        }
    }

    private void sendQuietly(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data, MediaType.TEXT_PLAIN));
        } catch (Exception ignored) {
            // 客户端断开
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 供接口暴露：当前可用技能 */
    public List<String> skillNames() {
        return skillRegistry.names();
    }
}
