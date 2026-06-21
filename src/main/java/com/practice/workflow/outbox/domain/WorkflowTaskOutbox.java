package com.practice.workflow.outbox.domain;

import com.practice.workflow.outbox.enums.OutboxEventType;
import com.practice.workflow.outbox.enums.OutboxStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流事件 Outbox 实体
 *
 * <p>用于存储待发送的 MQ 消息事件，实现 Outbox 模式确保消息可靠投递。
 * 事件随本地事务写入，由后台任务轮询发送至 RabbitMQ，发送成功后标记状态。
 *
 * <p>生命周期：PENDING → SENT / FAILED
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/21 10:31
 */
@Data
public class WorkflowTaskOutbox {

    /**
     * 数据库主键，自增
     */
    private Long id;

    /**
     * 关联的工作流任务 ID
     */
    private Long taskId;

    /**
     * 业务幂等键
     * <p>用于防止重复发送同一条消息，与 workflow_task.bizKey 对应。
     */
    private String bizKey;


    private String messageKey;

    /**
     * 事件类型
     * <p>标识消息的业务含义，如 "TASK_CREATED"、"TASK_FINISHED"、"TASK_FAILED" 等。
     */
    private OutboxEventType eventType;

    /**
     * 消息内容（JSON 格式）
     * <p>存储事件的完整业务数据，发送时作为 MQ Message Body。
     */
    private String payload;

    /**
     * 发送状态
     * <p>PENDING - 待发送；SENT - 已发送；FAILED - 发送失败
     */
    private OutboxStatus status;

    /**
     * 当前已重试次数，初始为 0
     */
    private Integer retryCount;

    /**
     * 最大允许重试次数，超过后状态置为 FAILED
     */
    private Integer maxRetry;

    /**
     * 最近一次发送失败的错误信息
     */
    private String lastError;

    /**
     * 事件创建时间（随业务事务写入）
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间（每次状态变更时刷新）
     */
    private LocalDateTime updatedAt;

    /**
     * 消息发送时间（成功发送至 MQ 的时间）
     */
    private LocalDateTime sentAt;
}
