package com.domye.picture.service.api.ai;

import java.util.List;
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
    /**
     * 发送消息并获取 AI 回复
     *
     * @param message   用户消息
     * @param imageUrls 图片 URL 列表（可选）
     * @return AI 回复
     */
    String chat(String message, List<String> imageUrls);
}
