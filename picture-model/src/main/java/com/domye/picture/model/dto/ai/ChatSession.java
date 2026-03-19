package com.domye.picture.model.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天会话
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID
     */
    private String id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 会话标题（自动生成或用户设置）
     */
    private String title;

    /**
     * 关联的空间 ID（可选）
     */
    private Long spaceId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 创建新会话
     */
    public static ChatSession create(Long userId, Long spaceId) {
        LocalDateTime now = LocalDateTime.now();
        return ChatSession.builder()
                .id(generateSessionId())
                .userId(userId)
                .spaceId(spaceId)
                .title("新对话")
                .createTime(now)
                .updateTime(now)
                .messageCount(0)
                .build();
    }

    /**
     * 生成会话 ID
     */
    private static String generateSessionId() {
        return "chat_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
    }
}