package com.mindharbor.psych.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private boolean enabled = true;

    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private String apiKey = "";

    private String model = "qwen-plus";

    private double temperature = 0.7;

    private int maxTokens = 1200;

    /** 有真实 key 才走大模型，否则走本地共情引擎 */
    public boolean available() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
