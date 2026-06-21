package com.practice.workflow.messaging.message;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流任务消息实体
 *
 * <p>通过 RabbitMQ 传递的工作流任务事件消息，包含任务标识、业务键、事件类型、追踪ID等。
 * 用于触发自动处理流程。
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/21 10:04
 */
@Data
public class WorkflowTaskMessage {

    /** 关联的工作流任务 ID */
    private Long taskId;

    /**
     * 业务幂等键
     * <p>与 workflow_task.bizKey 对应，用于消息幂等处理。
     */
    private String bizKey;

    /**
     * 事件类型
     * <p>标识消息的业务含义，如 AUTO_PROCESS_REQUESTED 表示请求自动处理。
     */
    private WorkflowTaskEventType eventType;

    /**
     * 追踪 ID
     * <p>用于日志追踪和问题排查，建议使用 UUID。
     */
    private String traceId;

    /** 消息创建时间 */
    private LocalDateTime createdAt;

}
