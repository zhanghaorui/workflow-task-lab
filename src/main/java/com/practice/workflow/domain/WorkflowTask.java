package com.practice.workflow.domain;

import com.practice.workflow.common.enums.WorkflowTaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流任务实体
 *
 * <p>描述一个完整的工作流任务的生命周期数据，包含业务标识、执行状态、
 * 重试信息、处理结果及审计时间戳。
 *
 * <p>生命周期状态流转参考 {@link WorkflowTaskStatus}。
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/8 22:34
 */
@Data
public class WorkflowTask {

    /** 数据库主键，由 Repository 层在插入后回填 */
    private Long id;

    /**
     * 业务幂等键（bizKey）
     * <p>由 projectId + bizType + bizId + sourceId 拼接而成，全局唯一，
     * 用于防止重复创建任务。
     */
    private String bizKey;

    /** 所属项目 ID */
    private Long projectId;

    /**
     * 业务类型
     * <p>用于区分同一项目下不同类型的工作流，如 "AUDIT"、"REVIEW" 等。
     */
    private String bizType;

    /** 业务对象 ID，指向具体需要处理的业务实体 */
    private String bizId;

    /**
     * 来源 ID
     * <p>标识触发本次任务的来源，如上游系统 ID、请求流水号等。
     */
    private String sourceId;

    /**
     * 任务当前状态
     *
     * @see WorkflowTaskStatus
     */
    private WorkflowTaskStatus status;

    /** 当前已重试次数，初始为 0 */
    private int retryCount;

    /** 最大允许重试次数，超过后任务将置为 {@link WorkflowTaskStatus#FAILED} */
    private int maxRetry;

    /**
     * 当前持有该任务的 Worker ID
     * <p>任务进入 PROCESSING 状态时由 tryStartTask 写入，用于防止其他 Worker 误操作。
     */
    private String workerId;

    /** 自动处理结果，由 Worker 在 finishAutoProcess 时写入 */
    private String autoResult;

    /** 人工审核结果，由审核人在 reviewTask 时写入 */
    private String manualResult;

    /** 最终结果，汇总自动处理结果与人工审核结果后写入 */
    private String finalResult;

    /** 最近一次失败的错误信息，便于排查问题 */
    private String errorMessage;

    /** 任务创建时间 */
    private LocalDateTime createdAt;

    /** 任务最后更新时间，每次状态变更时刷新 */
    private LocalDateTime updatedAt;

    /** 任务开始处理时间（进入 PROCESSING 状态的时间） */
    private LocalDateTime startedAt;

    /** 任务完成时间（进入终态的时间） */
    private LocalDateTime finishedAt;
}
