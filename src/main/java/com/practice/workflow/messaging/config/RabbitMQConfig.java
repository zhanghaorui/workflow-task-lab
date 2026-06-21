package com.practice.workflow.messaging.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>TODO</p>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/21 10:04
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 主交换机：用于投递工作流任务消息
     */
    public static final String WORKFLOW_TASK_EXCHANGE = "workflow.task.exchange";

    /**
     * 自动处理任务队列
     */
    public static final String AUTO_PROCESS_QUEUE = "workflow.task.auto-process.queue";

    /**
     * 自动处理任务 routing key
     */
    public static final String AUTO_PROCESS_ROUTING_KEY = "workflow.task.auto-process.routing-key";

    /**
     * 死信交换机
     */
    public static final String WORKFLOW_TASK_DLX = "workflow.task.dlx";

    /**
     * 自动处理任务死信队列
     */
    public static final String AUTO_PROCESS_DLQ = "workflow.task.auto-process.dlq";

    /**
     * 自动处理任务死信 routing key
     */
    public static final String AUTO_PROCESS_DLQ_ROUTING_KEY = "workflow.task.auto-process.dlq-routing-key";

    /**
     * RabbitAdmin 用于在应用启动后声明 exchange / queue / binding。
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * 主交换机。
     */
    @Bean
    public DirectExchange workflowTaskExchange() {
        return ExchangeBuilder
                .directExchange(WORKFLOW_TASK_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 死信交换机。
     */
    @Bean
    public DirectExchange workflowTaskDeadLetterExchange() {
        return ExchangeBuilder
                .directExchange(WORKFLOW_TASK_DLX)
                .durable(true)
                .build();
    }

    /**
     * 自动处理任务队列。
     * <p>
     * 这里配置了死信交换机和死信 routing key。
     * 后续如果消息被拒绝且不重新入队，或者 TTL / 队列策略触发死信，
     * RabbitMQ 会把消息路由到 DLX。
     */
    @Bean
    public Queue autoProcessQueue() {
        return QueueBuilder
                .durable(AUTO_PROCESS_QUEUE)
                .deadLetterExchange(WORKFLOW_TASK_DLX)
                .deadLetterRoutingKey(AUTO_PROCESS_DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 自动处理任务死信队列。
     */
    @Bean
    public Queue autoProcessDeadLetterQueue() {
        return QueueBuilder
                .durable(AUTO_PROCESS_DLQ)
                .build();
    }

    /**
     * 主交换机 -> 自动处理队列。
     */
    @Bean
    public Binding autoProcessBinding() {
        return BindingBuilder
                .bind(autoProcessQueue())
                .to(workflowTaskExchange())
                .with(AUTO_PROCESS_ROUTING_KEY);
    }

    /**
     * 死信交换机 -> 自动处理死信队列。
     */
    @Bean
    public Binding autoProcessDeadLetterBinding() {
        return BindingBuilder
                .bind(autoProcessDeadLetterQueue())
                .to(workflowTaskDeadLetterExchange())
                .with(AUTO_PROCESS_DLQ_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器。
     * <p>
     * Publisher 可以直接发送 Java 对象，
     * Consumer 也可以直接接收 Java 对象。
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitListener 使用的容器工厂。
     * <p>
     * 第一版只配置 JSON 转换器即可。
     * 并发、手动 ack、重试、异常处理后面再加。
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jackson2JsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter);
        return factory;
    }
}
