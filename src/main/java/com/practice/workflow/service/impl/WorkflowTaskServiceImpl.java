package com.practice.workflow.service.impl;

import com.practice.workflow.common.enums.BizErrorCode;
import com.practice.workflow.common.enums.WorkflowTaskStatus;
import com.practice.workflow.common.exception.WorkflowTaskException;
import com.practice.workflow.domain.WorkflowTask;
import com.practice.workflow.repository.WorkflowTaskRepository;
import com.practice.workflow.service.WorkflowTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
        String bizKey = assembleBizKey(projectId, bizType, bizId, sourceId);
        WorkflowTask workflowTask = workflowTaskRepository.findWorkflowTaskByBizKey(bizKey);
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
        assembleAuditParam(workflowTask);
        long id = workflowTaskRepository.insertTask(workflowTask);
        workflowTask.setId(id);
        return workflowTask;
    }

    @Override
    public boolean tryStartTask(Long taskId, String workerId) {
        if (Objects.isNull(taskId) || Objects.isNull(workerId)) {
            return false;
        }
        WorkflowTask task = workflowTaskRepository.getWorkflowTaskById(taskId);
        if (Objects.isNull(task)) {
            return false;
        }
        synchronized (task) {
            WorkflowTaskStatus status = task.getStatus();

            if (!Objects.equals(status, WorkflowTaskStatus.WAITING) && !Objects.equals(status, WorkflowTaskStatus.AUTO_PROCESS_FAILED)) {
                return false;
            }

            if (task.getRetryCount() >= task.getMaxRetry()) {
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            task.setStatus(WorkflowTaskStatus.PROCESSING);
            task.setWorkerId(workerId);
            task.setStartedAt(now);
            task.setUpdatedAt(now);
            return true;
        }
    }

    @Override
    public void failTask(Long taskId, String workerId, String errorMessage) {
        if (Objects.isNull(taskId) || StringUtils.isEmpty(workerId)) {
            return;
        }
        WorkflowTask task = workflowTaskRepository.getWorkflowTaskById(taskId);
        if (Objects.isNull(task)) {
            return;
        }
        synchronized (task) {
            if (Objects.equals(WorkflowTaskStatus.PROCESSING, task.getStatus())) {
                if (Objects.equals(task.getWorkerId(), workerId)) {
                    int retryCount = task.getRetryCount();
                    if ((retryCount + 1) < task.getMaxRetry()) {
                        task.setStatus(WorkflowTaskStatus.AUTO_PROCESS_FAILED);
                        task.setRetryCount(retryCount + 1);
                        task.setUpdatedAt(LocalDateTime.now());
                        task.setErrorMessage(errorMessage);
                    } else {
                        task.setStatus(WorkflowTaskStatus.FAILED);
                        task.setUpdatedAt(LocalDateTime.now());
                        task.setRetryCount(retryCount + 1);
                        task.setErrorMessage(errorMessage);
                        task.setFinishedAt(LocalDateTime.now());
                    }
                }
            }

        }
    }

    @Override
    public void finishAutoProcess(Long taskId, String workerId, String autoResult) {
        if (Objects.isNull(taskId) || !StringUtils.hasText(workerId) || !StringUtils.hasText(autoResult)) {
            return;
        }
        WorkflowTask task = workflowTaskRepository.getWorkflowTaskById(taskId);
        if (Objects.isNull(task)) {
            return;
        }
        synchronized (task) {
            if (Objects.equals(workerId, task.getWorkerId())) {
                if (Objects.equals(task.getStatus(), WorkflowTaskStatus.PROCESSING)) {
                    task.setStatus(WorkflowTaskStatus.WAIT_MANUAL_REVIEW);
                    task.setFinishedAt(LocalDateTime.now());
                    task.setAutoResult(autoResult);
                    task.setUpdatedAt(LocalDateTime.now());
                }
            }
        }
    }

    @Override
    public void reviewTask(Long taskId, String reviewerId, boolean approved, String manualResult) {
        if (Objects.isNull(taskId) || !StringUtils.hasText(reviewerId) || !StringUtils.hasText(manualResult)) {
            throw WorkflowTaskException.of(BizErrorCode.PARAM_INVALID);
        }
        WorkflowTask workflowTaskById = workflowTaskRepository.getWorkflowTaskById(taskId);
        if (Objects.isNull(workflowTaskById)) {
            throw WorkflowTaskException.of(BizErrorCode.TASK_NOT_FOUND);
        }
        synchronized (workflowTaskById) {
            if (Objects.equals(workflowTaskById.getStatus(), WorkflowTaskStatus.WAIT_MANUAL_REVIEW)) {
                workflowTaskById.setFinishedAt(LocalDateTime.now());
                workflowTaskById.setManualResult(manualResult);
                workflowTaskById.setFinalResult(manualResult);
                workflowTaskById.setUpdatedAt(LocalDateTime.now());
                workflowTaskById.setStatus(approved ? WorkflowTaskStatus.REVIEW_CONFIRMED : WorkflowTaskStatus.REVIEW_REJECTED);
            }
        }

    }


    private static String assembleBizKey(Long projectId, String bizType, String bizId, String sourceId) {
        return TASK_KEY + projectId.toString() + COLON + bizType + COLON + bizId + COLON + sourceId;
    }


    private static void assembleAuditParam(WorkflowTask workflowTask) {
        workflowTask.setCreatedAt(LocalDateTime.now());
        workflowTask.setUpdatedAt(LocalDateTime.now());
    }
}

