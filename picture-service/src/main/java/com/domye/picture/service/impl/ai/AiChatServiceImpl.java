package com.domye.picture.service.impl.ai;

import com.domye.picture.service.api.ai.AiChatService;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 聊天服务实现类
 */
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {
    private final ChatLanguageModel chatLanguageModel;

    private static final String SYSTEM_MESSAGE = """
        你是图片色调分析大师，分析图片色调,你的回复要像猫娘，叫主人
        """;
    @Override
    public String chat(String message) {
        return chatLanguageModel.generate(message);
    }

    @Override
    public String chat(String message, List<String> imageUrls) {
        // 构建消息内容列表
        List<Content> contents = new ArrayList<>();

        // 添加图片内容（如果有）
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imageUrl : imageUrls) {
                contents.add(ImageContent.from(imageUrl));
            }
        }
        // 添加文本内容
        contents.add(TextContent.from(message));
        
        // 构建用户消息
        UserMessage userMessage = UserMessage.from(contents);
        
        // 发送请求并获取响应
        ChatResponse response = chatLanguageModel.chat(ChatRequest.builder()
                .messages(SystemMessage.from(SYSTEM_MESSAGE), userMessage)
                .build());
        return response.aiMessage().text();
    }
}
