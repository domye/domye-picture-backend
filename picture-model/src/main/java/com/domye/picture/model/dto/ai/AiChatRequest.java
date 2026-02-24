package com.domye.picture.model.dto.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 聊天请求
 */
@Data
public class AiChatRequest implements Serializable {

    /**
     * 用户消息
     */
    private String message;
    /**
     * 图片 URL 列��（用于多模态图片理解）
     */
    private List<String> imageUrls;
}
