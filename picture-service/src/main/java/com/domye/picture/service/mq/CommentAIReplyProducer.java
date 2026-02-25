package com.domye.picture.service.mq;

import com.domye.picture.model.message.CommentAIReplyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * AI评论回复消息生产者
 * 负责发送AI回复请求到RocketMQ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentAIReplyProducer {

    private static final String TOPIC = "comment-ai-reply-topic";

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送AI回复请求消息
     *
     * @param message AI回复请求消息
     */
    public void sendAIReplyRequest(CommentAIReplyMessage message) {
        try {
            // 增加超时时间到 10 秒
            rocketMQTemplate.syncSend(
                    TOPIC,
                    MessageBuilder.withPayload(message).build(),
                    10000
            );
            log.info("[AI回复] 消息发送成功: commentId={}, pictureId={}", 
                    message.getCommentId(), message.getPictureId());
        } catch (Exception e) {
            log.error("[AI回复] 消息发送失败: commentId={}, pictureId={}, error={}", 
                    message.getCommentId(), message.getPictureId(), e.getMessage(), e);
        }
    }
}
