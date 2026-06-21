package com.practice.workflow.messaging.publisher;

import com.practice.workflow.messaging.config.RabbitMQConfig;
import com.practice.workflow.messaging.message.WorkflowTaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工作流任务消息发送服务
 *
 * <p>负责将 WorkflowTaskMessage 发送至 RabbitMQ，使用 Outbox 模式确保消息可靠投递。
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/21 10:05
 */
@Service
@Slf4j
public class WorkflowTaskMessagePublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送工作流任务消息至 RabbitMQ
     *
     <p>消息将发送至 {@link RabbitMQConfig#WORKFLOW_TASK_EXCHANGE} 交换机，
     * 使用 {@link RabbitMQConfig#AUTO_PROCESS_ROUTING_KEY} 路由键。
     *
     * <p>RabbitTemplate 已配置 JSON 消息转换器，可直接发送 Java 对象。
     *
     * @param message 工作流任务消息
     */
    public void publishTaskMessage(WorkflowTaskMessage message) {
        log.info("[Publisher] 发送工作流任务消息: taskId={}, bizKey={}, eventType={}, traceId={}",
                message.getTaskId(), message.getBizKey(), message.getEventType(), message.getTraceId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.WORKFLOW_TASK_EXCHANGE,
                RabbitMQConfig.AUTO_PROCESS_ROUTING_KEY,
                message
        );

        log.info("[Publisher] 消息发送成功: taskId={}, traceId={}", message.getTaskId(), message.getTraceId());
    }
}
