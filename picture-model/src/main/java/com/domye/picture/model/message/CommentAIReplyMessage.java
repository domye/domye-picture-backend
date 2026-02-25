package com.domye.picture.model.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * AI评论回复消息
 * 用于RocketMQ传递AI回复请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentAIReplyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 触发评论ID
     */
    private Long commentId;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;
}
