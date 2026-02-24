package com.domye.picture.api.config.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class LangChain4jConfig {

    /**
     * OpenAI API Key
     */
    private String apiKey;

    /**
     * API 基础 URL（支持自定义 OpenAI 兼容接口）
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 模型名称
     */
    private String modelName = "gpt-3.5-turbo";

    /**
     * 温度参数，控制回答的随机性
     */
    private Double temperature = 0.7;

    /**
     * 最大 Token 数
     */
    private Integer maxTokens = 2000;

    /**
     * 请求超时时间（秒）
     */
    private Integer timeout = 60;

    /**
     * 创建 ChatLanguageModel Bean
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(java.time.Duration.ofSeconds(timeout))
                .build();
    }
}
