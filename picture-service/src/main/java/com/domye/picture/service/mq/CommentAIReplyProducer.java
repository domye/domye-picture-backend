package com.domye.picture.service.mq;

import com.domye.picture.model.message.CommentAIReplyMessage;
import com.domye.picture.service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * AI评论回复消息生产者
 * 负责发送AI回复请求到RabbitMQ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentAIReplyProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送AI回复请求消息
     *
     * @param message AI回复请求消息
     */
    public void sendAIReplyRequest(CommentAIReplyMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    message
            );
            log.info("[AI回复] 消息发送成功: commentId={}, pictureId={}",
                    message.getCommentId(), message.getPictureId());
        } catch (Exception e) {
            log.error("[AI回复] 消息发送失败: commentId={}, pictureId={}, error={}",
                    message.getCommentId(), message.getPictureId(), e.getMessage(), e);
        }
    }
}
