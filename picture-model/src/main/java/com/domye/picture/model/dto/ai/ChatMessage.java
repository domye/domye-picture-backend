package com.domye.picture.model.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息 ID
     */
    private String id;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 消息角色：user / assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建用户消息
     */
    public static ChatMessage userMessage(String sessionId, String content) {
        return ChatMessage.builder()
                .id(generateId())
                .sessionId(sessionId)
                .role("user")
                .content(content)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建助手消息
     */
    public static ChatMessage assistantMessage(String sessionId, String content) {
        return ChatMessage.builder()
                .id(generateId())
                .sessionId(sessionId)
                .role("assistant")
                .content(content)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 生成唯一 ID
     */
    private static String generateId() {
        return String.valueOf(System.currentTimeMillis());
    }
}