package com.domye.picture.model.dto.ai;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 聊天请求
 */
@Data
public class AiChatRequest implements Serializable {

    /**
     * 用户消息
     */
    private String message;
}
