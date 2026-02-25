package com.domye.picture.service.ai;

import com.domye.picture.model.message.CommentAIReplyMessage;

/**
 * AI评论回复服务接口
 */
public interface CommentAIReplyService {

    /**
     * 生成AI回复
     *
     * @param message AI回复请求消息
     * @return AI生成的回复内容
     */
    String generateReply(CommentAIReplyMessage message);
}
