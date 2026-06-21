package com.practice.workflow.service.impl;

import com.practice.workflow.common.enums.BizErrorCode;
import com.practice.workflow.common.enums.WorkflowTaskStatus;
import com.practice.workflow.common.exception.WorkflowTaskException;
import com.practice.workflow.domain.WorkflowTask;
import com.practice.workflow.repository.WorkflowTaskRepository;
import com.practice.workflow.service.WorkflowTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * <p>TODO</p>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/8 22:40
 */
@Service
@Slf4j
public class WorkflowTaskServiceImpl implements WorkflowTaskService {

    public static final String TASK_KEY = "task:";


    public static final String COLON = ":";


    public static final Integer MAX_RETRY_COUNT = 3;

    @Autowired
    private WorkflowTaskRepository workflowTaskRepository;


    /**
     * 根据 projectId + bizType + bizId + sourceId 生成 bizKey
     * 如果 bizKey 已存在，返回已有任务
     * 如果不存在，创建新任务
     * 初始状态 WAITING
     * retryCount = 0
     * maxRetry = 3
     *
     * @param projectId
     * @param bizType
     * @param bizId
     * @param sourceId
     * @return
     */
    @Override
    public WorkflowTask createTask(Long projectId, String bizType, String bizId, String sourceId) {
        if (Objects.isNull(projectId) || !StringUtils.hasText(bizType) || !StringUtils.hasText(bizId) || !StringUtils.hasText(sourceId)) {
            throw WorkflowTaskException.of(BizErrorCode.PARAM_INVALID);
        }
        String bizKey = assembleBizKey(projectId, bizType, bizId, sourceId);
        WorkflowTask workflowTask = workflowTaskRepository.findByBizKey(bizKey);
        if (!Objects.isNull(workflowTask)) {
            return workflowTask;
        }
        workflowTask = new WorkflowTask();
        workflowTask.setBizKey(bizKey);
        workflowTask.setBizId(bizId);
        workflowTask.setSourceId(sourceId);
        workflowTask.setBizType(bizType);
        workflowTask.setProjectId(projectId);
        workflowTask.setStatus(WorkflowTaskStatus.WAITING);
        workflowTask.setMaxRetry(MAX_RETRY_COUNT);
        workflowTask.setRetryCount(0);
        workflowTask.setCreatedAt(LocalDateTime.now());
        workflowTask.setUpdatedAt(LocalDateTime.now());
        long id;
        try {
            id = workflowTaskRepository.insertWorkFlowTask(workflowTask);
            workflowTask.setId(id);
        } catch (DuplicateKeyException e) {
            log.info("检测到唯一索引冲突，查询一次");
            workflowTask = workflowTaskRepository.findByBizKey(bizKey);
            if (!Objects.isNull(workflowTask)) {
                return workflowTask;
            } else {
                throw e;
            }
        }
        return workflowTask;
    }

    @Override
    public boolean tryStartTask(Long taskId, String workerId) {
        if (Objects.isNull(taskId) || Objects.isNull(workerId)) {
            throw WorkflowTaskException.of(BizErrorCode.PARAM_INVALID);
        }
        WorkflowTask task = workflowTaskRepository.findById(taskId);
        if (Objects.isNull(task)) {
            throw WorkflowTaskException.of(BizErrorCode.TASK_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        task.setWorkerId(workerId);
        task.setStartedAt(now);
        task.setUpdatedAt(now);
        int i = workflowTaskRepository.tryStartTask(task);
        return i > 0;
    }

    /**
     * 标记任务失败
     * <p>
     * 条件：任务状态为 PROCESSING 且 workerId 匹配
     * 失败后 retryCount + 1：
     * - 如果新的 retryCount < maxRetry：status = AUTO_PROCESS_FAILED
     * - 如果新的 retryCount >= maxRetry：status = FAILED，设置 finishedAt
     *
     * @param taskId       任务 ID
     * @param workerId     当前持有者 ID
     * @param errorMessage 错误信息
     */
    @Override
    public void failTask(Long taskId, String workerId, String errorMessage) {
        if (Objects.isNull(taskId) || !StringUtils.hasText(workerId)) {
            throw WorkflowTaskException.of(BizErrorCode.PARAM_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        int affectedRows = workflowTaskRepository.failTask(taskId, workerId, errorMessage, now, now);

        if (affectedRows == 0) {
            // 更新失败，可能是状态不正确或 workerId 不匹配
            WorkflowTask task = workflowTaskRepository.findById(taskId);
            if (Objects.isNull(task)) {
                throw WorkflowTaskException.of(BizErrorCode.TASK_NOT_FOUND);
            }
            if (!Objects.equals(WorkflowTaskStatus.PROCESSING, task.getStatus())) {
                throw WorkflowTaskException.of(BizErrorCode.TASK_STATUS_ILLEGAL);
            }
            if (!Objects.equals(task.getWorkerId(), workerId)) {
                throw WorkflowTaskException.of(BizErrorCode.TASK_WORKER_MISMATCH);
            }
            // 如果状态和 workerId 都正确但更新失败，抛出异常
            throw new IllegalStateException("任务更新失败，请重试");
        }
    }

    @Override
    public void finishAutoProcess(Long taskId, String workerId, String autoResult) {
        if (Objects.isNull(taskId) || !StringUtils.hasText(workerId) || !StringUtils.hasText(autoResult)) {
            throw WorkflowTaskException.of(BizErrorCode.PARAM_INVALID);
        }

        WorkflowTask task = workflowTaskRepository.findById(taskId);
        if (Objects.isNull(task)) {
            throw WorkflowTaskException.of(BizErrorCode.TASK_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        int affectedRows = workflowTaskRepository.finishAutoProcess(taskId, workerId, autoResult, now);
        if (affectedRows == 0) {
            // 更新失败，检查原因
            WorkflowTask currentTask = workflowTaskRepository.findById(taskId);
            if (Objects.isNull(currentTask)) {
                throw WorkflowTaskException.of(BizErrorCode.TASK_NOT_FOUND);
            }
            if (!Objects.equals(WorkflowTaskStatus.PROCESSING, currentTask.getStatus())) {
                throw WorkflowTaskException.of(BizErrorCode.TASK_STATUS_ILLEGAL);
            }
            if (!Objects.equals(currentTask.getWorkerId(), workerId)) {
                throw WorkflowTaskException.of(BizErrorCode.TASK_WORKER_MISMATCH);
            }
            throw new IllegalStateException("任务更新失败，请重试");
        }
    }

    @Override
    public void reviewTask(Long taskId, String reviewerId, boolean approved, String manualResult) {
        if (Objects.isNull(taskId) || !StringUtils.hasText(reviewerId) || !StringUtils.hasText(manualResult)) {
            throw WorkflowTaskException.of(BizErrorCode.PARAM_INVALID);
        }

        WorkflowTask task = workflowTaskRepository.findById(taskId);
        if (Objects.isNull(task)) {
            throw WorkflowTaskException.of(BizErrorCode.TASK_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        task.setFinishedAt(now);
        task.setManualResult(manualResult);
        task.setFinalResult(manualResult);
        task.setUpdatedAt(now);
        task.setStatus(approved ? WorkflowTaskStatus.REVIEW_CONFIRMED : WorkflowTaskStatus.REVIEW_REJECTED);

        int affectedRows = workflowTaskRepository.reviewTask(task);
        if (affectedRows == 0) {
            // 更新失败，检查原因
            WorkflowTask currentTask = workflowTaskRepository.findById(taskId);
            if (Objects.isNull(currentTask)) {
                throw WorkflowTaskException.of(BizErrorCode.TASK_NOT_FOUND);
            }
            if (!Objects.equals(WorkflowTaskStatus.WAIT_MANUAL_REVIEW, currentTask.getStatus())) {
                throw WorkflowTaskException.of(BizErrorCode.TASK_STATUS_ILLEGAL);
            }
            throw new IllegalStateException("任务更新失败，请重试");
        }
    }

    private static String assembleBizKey(Long projectId, String bizType, String bizId, String sourceId) {
        return TASK_KEY + projectId.toString() + COLON + bizType + COLON + bizId + COLON + sourceId;
    }
}

