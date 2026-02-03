package com.domye.picture.service.vote.rocketMQ;

import com.domye.picture.service.vote.model.dto.VoteEndRequest;
import com.domye.picture.service.vote.model.dto.VoteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteProducer {
  final RocketMQTemplate rocketMQTemplate;

    public void sendVoteMessage(VoteRequest request) {
        String topic = "vote_topic";
        log.info("准备发送投票消息到主题: {}, 内容: {}", topic, request);
        rocketMQTemplate.asyncSend(topic, request, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("发送投票消息成功：{}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("发送投票消息失败", e);
            }
        });
    }

    /**
     * 发送投票活动结束的延迟消息
     * @param request     投票活动结束请求
     * @param delayMillis 延迟时间（毫秒）
     */
    public void sendVoteEndDelayMessage(VoteEndRequest request, long delayMillis) {
        String topic = "vote_end_topic";
        log.info("准备发送投票活动结束延迟消息到主题: {}, 延迟时间: {}ms, 内容: {}", topic, delayMillis, request);

        // 创建消息
        Message<VoteEndRequest> message = MessageBuilder.withPayload(request).build();

        // 发送延迟消息
        rocketMQTemplate.asyncSend(topic, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("发送投票活动结束延迟消息成功：{}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("发送投票活动结束延迟消息失败", e);
            }
        }, delayMillis);

    }
}
