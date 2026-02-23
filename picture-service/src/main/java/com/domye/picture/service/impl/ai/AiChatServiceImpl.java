package com.domye.picture.service.impl.ai;

import com.domye.picture.service.api.ai.AiChatService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 聊天服务实现类
 */
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatLanguageModel chatLanguageModel;

    @Override
    public String chat(String message) {
        return chatLanguageModel.generate(message);
    }
}
