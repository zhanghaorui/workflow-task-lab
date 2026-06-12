package com.practice.workflow.domain;

import com.practice.workflow.common.enums.WorkflowTaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>TODO</p>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/8 22:34
 */
@Data
public class WorkflowTask {

    /**
     *  DB主键
     */
    private Long id;

    /**
     *  业务幂等key
     */
    private String bizKey;

    /**
     *  项目ID
     */
    private Long projectId;
    private String bizType;
    private String bizId;
    private String sourceId;

    private WorkflowTaskStatus status;

    private int retryCount;
    private int maxRetry;

    private String workerId;
    private String autoResult;
    private String manualResult;
    private String finalResult;
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

}
