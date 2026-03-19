package com.domye.picture.service.api.ai;

import com.domye.picture.model.dto.ai.ChatSession;
import com.domye.picture.model.dto.ai.ChatStreamRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 聊天服务接口
 * 负责处理流式聊天逻辑
 */
public interface AiChatService {

    /**
     * 流式聊天
     * 通过 SSE 推送 AI 响应
     *
     * @param userId  用户 ID
     * @param request 聊天请求
     * @return SSE Emitter
     */
    SseEmitter streamChat(Long userId, ChatStreamRequest request);

    /**
     * 创建新会话
     *
     * @param userId  用户 ID
     * @param spaceId 空间 ID（可选）
     * @return 新创建的会话
     */
    ChatSession createSession(Long userId, Long spaceId);

    /**
     * 获取用户的会话列表
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    java.util.List<ChatSession> getUserSessions(Long userId);

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     * @param userId    用户 ID（用于权限验证）
     */
    void deleteSession(String sessionId, Long userId);
}