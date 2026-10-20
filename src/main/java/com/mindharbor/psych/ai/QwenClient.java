package com.mindharbor.psych.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/**
 * 通义千问 DashScope（OpenAI 兼容模式）流式客户端。
 * 支持 tools / function calling，并能在流式响应里拼装分片的 tool_calls。
 */
@Slf4j
@Component
public class QwenClient {

    public record ToolCall(String id, String name, String arguments) {
    }

    public record StreamOutcome(String content, List<ToolCall> toolCalls) {
    }

    private final AiProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http;

    public QwenClient(AiProperties props) {
        this.props = props;
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15));
        // Java 不会自动读取 http_proxy 环境变量，这里手动接管
        String proxy = System.getenv("https_proxy");
        if (proxy == null || proxy.isBlank()) {
            proxy = System.getenv("HTTPS_PROXY");
        }
        if (proxy != null && !proxy.isBlank()) {
            try {
                URI p = URI.create(proxy.contains("://") ? proxy : "http://" + proxy);
                if (p.getHost() != null) {
                    builder.proxy(ProxySelector.of(new InetSocketAddress(p.getHost(),
                            p.getPort() > 0 ? p.getPort() : 80)));
                    log.info("AI 客户端使用代理 {}:{}", p.getHost(), p.getPort());
                }
            } catch (Exception e) {
                log.warn("代理地址解析失败，改为直连：{}", proxy);
            }
        }
        this.http = builder.build();
    }

    public boolean available() {
        return props.available();
    }

    /**
     * 流式对话（带工具）。
     *
     * @param messages 完整上下文，元素形如 {"role":"user","content":"..."}
     * @param tools    OpenAI tools 数组，可为 null
     * @return 本轮完整文本 + 模型请求调用的工具列表
     */
    public StreamOutcome stream(List<Map<String, Object>> messages,
                                List<Map<String, Object>> tools,
                                Consumer<String> onDelta,
                                Consumer<String> onError) {
        StringBuilder full = new StringBuilder();
        // tool_calls 在流式响应里是按 index 分片下发的，需要逐个拼装
        Map<Integer, String> idBuf = new HashMap<>();
        Map<Integer, String> nameBuf = new HashMap<>();
        Map<Integer, StringBuilder> argBuf = new TreeMap<>();

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", props.getModel());
            body.put("messages", messages);
            body.put("stream", true);
            body.put("temperature", props.getTemperature());
            body.put("max_tokens", props.getMaxTokens());
            if (tools != null && !tools.isEmpty()) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(props.getBaseUrl() + "/chat/completions"))
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.warn("大模型返回 {}: {}", response.statusCode(), err);
                onError.accept("大模型服务暂时不可用（HTTP " + response.statusCode() + "）");
                return new StreamOutcome("", List.of());
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) {
                        continue;
                    }
                    JsonNode node = mapper.readTree(data);
                    JsonNode choices = node.path("choices");
                    if (!choices.isArray() || choices.isEmpty()) {
                        continue;
                    }
                    JsonNode delta = choices.get(0).path("delta");

                    String text = delta.path("content").asText("");
                    if (!text.isEmpty()) {
                        full.append(text);
                        onDelta.accept(text);
                    }

                    JsonNode calls = delta.path("tool_calls");
                    if (calls.isArray()) {
                        for (JsonNode c : calls) {
                            int idx = c.path("index").asInt(0);
                            if (c.hasNonNull("id")) {
                                idBuf.putIfAbsent(idx, c.get("id").asText());
                            }
                            JsonNode fn = c.path("function");
                            if (fn.hasNonNull("name")) {
                                nameBuf.putIfAbsent(idx, fn.get("name").asText());
                            }
                            if (fn.hasNonNull("arguments")) {
                                argBuf.computeIfAbsent(idx, k -> new StringBuilder())
                                        .append(fn.get("arguments").asText());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("调用大模型失败", e);
            onError.accept("连接大模型失败：" + e.getMessage());
            return new StreamOutcome(full.toString(), List.of());
        }

        List<ToolCall> toolCalls = new ArrayList<>();
        for (Map.Entry<Integer, StringBuilder> e : argBuf.entrySet()) {
            String name = nameBuf.get(e.getKey());
            if (name == null) {
                continue;
            }
            toolCalls.add(new ToolCall(
                    idBuf.getOrDefault(e.getKey(), "call_" + e.getKey()),
                    name,
                    e.getValue().toString()));
        }
        return new StreamOutcome(full.toString(), toolCalls);
    }

    /** 非流式一次性调用，用于「AI 辅助生成评估报告」这类任务 */
    public String complete(String systemPrompt, String userPrompt) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        StringBuilder sb = new StringBuilder();
        stream(messages, null, sb::append, msg -> log.warn(msg));
        return sb.toString();
    }
}
