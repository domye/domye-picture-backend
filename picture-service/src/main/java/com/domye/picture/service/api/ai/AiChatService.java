package com.domye.picture.service.api.ai;

/**
 * AI 聊天服务接口
 */
public interface AiChatService {

    /**
     * 发送消息并获取 AI 回复
     *
     * @param message 用户消息
     * @return AI 回复
     */
    String chat(String message);
}
