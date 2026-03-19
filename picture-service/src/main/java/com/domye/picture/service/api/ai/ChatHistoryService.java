package com.domye.picture.service.api.ai;

import com.domye.picture.model.dto.ai.ChatMessage;
import com.domye.picture.model.dto.ai.ChatSession;

import java.util.List;

/**
 * 聊天历史管理服务接口
 * 基于 Redis 实现会话历史管理
 */
public interface ChatHistoryService {

    /**
     * 创建新会话
     *
     * @param userId  用户 ID
     * @param spaceId 空间 ID（可选）
     * @return 新创建的会话
     */
    ChatSession createSession(Long userId, Long spaceId);

    /**
     * 获取会话信息
     *
     * @param sessionId 会话 ID
     * @return 会话信息，不存在返回 null
     */
    ChatSession getSession(String sessionId);

    /**
     * 添加消息到会话
     *
     * @param sessionId 会话 ID
     * @param message   聊天消息
     */
    void addMessage(String sessionId, ChatMessage message);

    /**
     * 获取会话的历史消息
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<ChatMessage> getMessages(String sessionId);

    /**
     * 获取会话的最近 N 条消息
     *
     * @param sessionId 会话 ID
     * @param limit     最大数量
     * @return 消息列表
     */
    List<ChatMessage> getRecentMessages(String sessionId, int limit);

    /**
     * 清除会话历史
     *
     * @param sessionId 会话 ID
     */
    void clearSession(String sessionId);

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     */
    void deleteSession(String sessionId);

    /**
     * 获取用户的所有会话列表
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    List<ChatSession> getUserSessions(Long userId);

    /**
     * 更新会话标题
     *
     * @param sessionId 会话 ID
     * @param title     新标题
     */
    void updateSessionTitle(String sessionId, String title);

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话 ID
     * @return 是否存在
     */
    boolean sessionExists(String sessionId);

    /**
     * 验证用户是否有权访问该会话
     *
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     * @return 是否有权访问
     */
    boolean hasAccess(String sessionId, Long userId);
}