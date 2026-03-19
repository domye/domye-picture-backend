package com.domye.picture.api.config.ai;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置类
 */
@Data
@Slf4j
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
     * Embedding 配置
     */
    private EmbeddingConfig embedding = new EmbeddingConfig();

    /**
     * Embedding 配置类
     */
    @Data
    public static class EmbeddingConfig {
        /**
         * Embedding 模型名称
         */
        private String modelName = "text-embedding-3-small";

        /**
         * 向量维度
         */
        private Integer dimension = 1536;
    }

    /**
     * 验证配置
     */
    @PostConstruct
    public void validate() {
        if (StrUtil.isBlank(apiKey) || "your-api-key-here".equals(apiKey)) {
            log.warn("AI API Key 未配置或使用默认值，AI 功能可能无法正常工作。请设置环境变量 AI_API_KEY");
        }
    }

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

    /**
     * 创建 StreamingChatLanguageModel Bean（用于 SSE 流式响应）
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(java.time.Duration.ofSeconds(timeout))
                .build();
    }

    /**
     * 创建 EmbeddingModel Bean（用于 RAG 向量化）
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embedding.getModelName())
                .build();
    }
}
