package com.domye.picture.service.vote.rocketMQ;

import com.domye.picture.service.vote.model.dto.VoteRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class VoteProducer {
    @Resource
    private RocketMQTemplate rocketMQTemplate;

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
}
