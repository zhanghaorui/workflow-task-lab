package com.practice.workflow.messaging.message;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>TODO</p>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/21 10:04
 */
@Data
public class WorkflowTaskMessage {

    private Long taskId;

    private String bizKey;

    private WorkflowTaskEventType eventType;

    private String traceId;

    private LocalDateTime createdAt;

}
