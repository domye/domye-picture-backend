package com.domye.picture.service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 声明队列、交换机和绑定关系
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 队列名称
     */
    public static final String QUEUE_NAME = "comment-ai-reply-queue";

    /**
     * 交换机名称
     */
    public static final String EXCHANGE_NAME = "comment-ai-reply.exchange";

    /**
     * 路由键
     */
    public static final String ROUTING_KEY = "comment-ai-reply.routing-key";

    /**
     * 声明队列（与服务器现有队列参数一致）
     */
    @Bean
    public Queue commentAIReplyQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-message-ttl", 86400000) // 24小时过期，与服务器一致
                .build();
    }

    /**
     * 声明直连交换机
     */
    @Bean
    public DirectExchange commentAIReplyExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    /**
     * 绑定队列和交换机
     */
    @Bean
    public Binding commentAIReplyBinding(Queue commentAIReplyQueue, DirectExchange commentAIReplyExchange) {
        return BindingBuilder
                .bind(commentAIReplyQueue)
                .to(commentAIReplyExchange)
                .with(ROUTING_KEY);
    }

    /**
     * 配置 JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setChannelTransacted(false);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 消息发送失败处理
                System.err.println("消息发送失败: " + cause);
            }
        });
        return template;
    }
}
