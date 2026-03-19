package com.domye.picture.service.impl.ai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.domye.picture.model.dto.ai.ChatMessage;
import com.domye.picture.model.dto.ai.ChatSession;
import com.domye.picture.service.api.ai.ChatHistoryService;
import com.domye.picture.common.helper.impl.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 聊天历史管理服务实现
 * 基于 Redis 实现会话历史管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final RedisCache redisCache;

    /**
     * Redis Key 前缀
     */
    private static final String KEY_PREFIX = "domye:ai:chat:";

    /**
     * 会话消息 Key 模板
     * 格式: domye:ai:chat:session:{sessionId}:messages
     */
    private static final String SESSION_MESSAGES_KEY = KEY_PREFIX + "session:%s:messages";

    /**
     * 会话信息 Key 模板
     * 格式: domye:ai:chat:session:{sessionId}:info
     */
    private static final String SESSION_INFO_KEY = KEY_PREFIX + "session:%s:info";

    /**
     * 用户会话列表 Key 模板
     * 格式: domye:ai:chat:user:{userId}:sessions
     */
    private static final String USER_SESSIONS_KEY = KEY_PREFIX + "user:%s:sessions";

    /**
     * 会话过期时间（7天）
     */
    private static final long SESSION_EXPIRE_DAYS = 7;

    /**
     * 最大消息历史数量
     */
    private static final int MAX_MESSAGE_HISTORY = 100;

    @Override
    public ChatSession createSession(Long userId, Long spaceId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        ChatSession session = ChatSession.create(userId, spaceId);

        // 保存会话信息
        saveSessionInfo(session);

        // 添加到用户会话列表
        addUserSession(userId, session.getId());

        log.info("创建新会话: sessionId={}, userId={}", session.getId(), userId);
        return session;
    }

    @Override
    public ChatSession getSession(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return null;
        }

        String key = String.format(SESSION_INFO_KEY, sessionId);
        String json = (String) redisCache.get(key);

        if (StrUtil.isBlank(json)) {
            return null;
        }

        return JSONUtil.toBean(json, ChatSession.class);
    }

    @Override
    public void addMessage(String sessionId, ChatMessage message) {
        if (StrUtil.isBlank(sessionId) || message == null) {
            return;
        }

        String key = String.format(SESSION_MESSAGES_KEY, sessionId);

        // 获取现有消息列表
        List<String> messages = getMessageJsonList(key);

        // 添加新消息
        messages.add(JSONUtil.toJsonStr(message));

        // 限制消息数量
        if (messages.size() > MAX_MESSAGE_HISTORY) {
            messages = messages.subList(messages.size() - MAX_MESSAGE_HISTORY, messages.size());
        }

        // 保存消息列表
        saveMessageList(key, messages);

        // 更新会话的最后更新时间和消息数量
        updateSessionOnMessage(sessionId);

        log.debug("添加消息到会话: sessionId={}, role={}", sessionId, message.getRole());
    }

    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return new ArrayList<>();
        }

        String key = String.format(SESSION_MESSAGES_KEY, sessionId);
        List<String> messagesJson = getMessageJsonList(key);

        return messagesJson.stream()
                .map(json -> JSONUtil.toBean(json, ChatMessage.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        List<ChatMessage> allMessages = getMessages(sessionId);

        if (allMessages.size() <= limit) {
            return allMessages;
        }

        return allMessages.subList(allMessages.size() - limit, allMessages.size());
    }

    @Override
    public void clearSession(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }

        String key = String.format(SESSION_MESSAGES_KEY, sessionId);
        redisCache.remove(key);

        // 更新会话信息
        ChatSession session = getSession(sessionId);
        if (session != null) {
            session.setMessageCount(0);
            session.setUpdateTime(LocalDateTime.now());
            saveSessionInfo(session);
        }

        log.info("清除会话消息历史: sessionId={}", sessionId);
    }

    @Override
    public void deleteSession(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }

        // 获取会话信息以获取 userId
        ChatSession session = getSession(sessionId);
        if (session != null) {
            // 从用户会话列表中移除
            removeUserSession(session.getUserId(), sessionId);
        }

        // 删除消息历史
        String messagesKey = String.format(SESSION_MESSAGES_KEY, sessionId);
        redisCache.remove(messagesKey);

        // 删除会话信息
        String infoKey = String.format(SESSION_INFO_KEY, sessionId);
        redisCache.remove(infoKey);

        log.info("删除会话: sessionId={}", sessionId);
    }

    @Override
    public List<ChatSession> getUserSessions(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        String key = String.format(USER_SESSIONS_KEY, userId);
        Set<Object> sessionIds = redisCache.sMembers(key);

        if (sessionIds == null || sessionIds.isEmpty()) {
            return new ArrayList<>();
        }

        return sessionIds.stream()
                .map(Object::toString)
                .map(this::getSession)
                .filter(Objects::nonNull)
                .sorted((s1, s2) -> s2.getUpdateTime().compareTo(s1.getUpdateTime())) // 按更新时间倒序
                .collect(Collectors.toList());
    }

    @Override
    public void updateSessionTitle(String sessionId, String title) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }

        ChatSession session = getSession(sessionId);
        if (session != null) {
            session.setTitle(title);
            session.setUpdateTime(LocalDateTime.now());
            saveSessionInfo(session);
        }
    }

    @Override
    public boolean sessionExists(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return false;
        }

        String key = String.format(SESSION_INFO_KEY, sessionId);
        return redisCache.hasKey(key);
    }

    @Override
    public boolean hasAccess(String sessionId, Long userId) {
        if (StrUtil.isBlank(sessionId) || userId == null) {
            return false;
        }

        ChatSession session = getSession(sessionId);
        return session != null && userId.equals(session.getUserId());
    }

    /**
     * 保存会话信息
     */
    private void saveSessionInfo(ChatSession session) {
        String key = String.format(SESSION_INFO_KEY, session.getId());
        String json = JSONUtil.toJsonStr(session);
        redisCache.put(key, json, SESSION_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 添加会话到用户会话列表
     */
    private void addUserSession(Long userId, String sessionId) {
        String key = String.format(USER_SESSIONS_KEY, userId);
        redisCache.sAdd(key, sessionId);
        redisCache.expireKey(key, SESSION_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 从用户会话列表中移除会话
     */
    private void removeUserSession(Long userId, String sessionId) {
        String key = String.format(USER_SESSIONS_KEY, userId);
        redisCache.sRemove(key, sessionId);
    }

    /**
     * 获取消息 JSON 列表
     */
    @SuppressWarnings("unchecked")
    private List<String> getMessageJsonList(String key) {
        Object value = redisCache.get(key);
        if (value == null) {
            return new ArrayList<>();
        }

        if (value instanceof String) {
            return JSONUtil.toList((String) value, String.class);
        }

        return new ArrayList<>();
    }

    /**
     * 保存消息列表
     */
    private void saveMessageList(String key, List<String> messages) {
        String json = JSONUtil.toJsonStr(messages);
        redisCache.put(key, json, SESSION_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 更新会话信息（添加消息后）
     */
    private void updateSessionOnMessage(String sessionId) {
        ChatSession session = getSession(sessionId);
        if (session != null) {
            session.setUpdateTime(LocalDateTime.now());
            session.setMessageCount(session.getMessageCount() + 1);
            saveSessionInfo(session);
        }
    }
}