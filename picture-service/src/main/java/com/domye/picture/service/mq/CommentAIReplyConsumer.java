package com.domye.picture.service.mq;

import com.domye.picture.model.dto.comment.CommentAddRequest;
import com.domye.picture.model.message.CommentAIReplyMessage;
import com.domye.picture.service.ai.CommentAIReplyService;
import com.domye.picture.service.api.comment.CommentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.domye.picture.service.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * AI评论回复消息消费者
 * 负责消费AI回复请求并调用AI服务生成回复
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentAIReplyConsumer {

    /**
     * AI用户ID
     */
    private static final Long AI_USER_ID = 2020004031158120450L;

    private final CommentAIReplyService commentAIReplyService;
    private final CommentsService commentsService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void onMessage(CommentAIReplyMessage message) {
        log.info("[AI回复] 收到消息: commentId={}, pictureId={}, content={}",
                message.getCommentId(), message.getPictureId(), message.getContent());

        try {
            // 1. 调用AI服务生成回复
            String aiReply = commentAIReplyService.generateReply(message);

            if (aiReply == null || aiReply.trim().isEmpty()) {
                log.warn("[AI回复] AI生成内容为空，跳过保存: commentId={}", message.getCommentId());
                return;
            }

            // 2. 构建AI回复评论请求
            CommentAddRequest replyRequest = new CommentAddRequest();
            replyRequest.setPictureid(message.getPictureId());
            replyRequest.setParentid(message.getCommentId()); // 回复触发评论
            replyRequest.setContent(aiReply);

            // 3. 保存AI回复评论（userId=0）
            commentsService.addComment(replyRequest, AI_USER_ID, null);

            log.info("[AI回复] AI回复保存成功: commentId={}, reply={}",
                    message.getCommentId(), aiReply);

        } catch (Exception e) {
            log.error("[AI回复] 处理失败: commentId={}, error={}",
                    message.getCommentId(), e.getMessage(), e);
            // 不抛出异常，避免RabbitMQ重试机制导致无限循环
        }
    }
}
