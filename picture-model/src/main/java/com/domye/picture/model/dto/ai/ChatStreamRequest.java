package com.domye.picture.model.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI 流式聊天请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID（可选，为空则创建新会话）
     */
    private String sessionId;

    /**
     * 用户消息
     */
    private String message;

    /**
     * 空间 ID（可选，用于限定搜索范围）
     */
    private Long spaceId;

    /**
     * 最大返回结果数（用于 RAG 检索）
     */
    @Builder.Default
    private Integer maxResults = 5;
}