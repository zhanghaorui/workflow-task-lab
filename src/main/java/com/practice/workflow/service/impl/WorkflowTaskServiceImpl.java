package com.practice.workflow.service.impl;

import com.practice.workflow.common.enums.WorkflowTaskStatus;
import com.practice.workflow.domain.WorkflowTask;
import com.practice.workflow.repository.WorkflowTaskRepository;
import com.practice.workflow.service.WorkflowTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        workflowTask.setBizKey(bizType);
        workflowTask.setProjectId(projectId);
        workflowTask.setStatus(WorkflowTaskStatus.WAITING);
        assembleAuditParam(workflowTask);
        workflowTaskRepository.insertTask(workflowTask);
        return workflowTask;
    }

    @Override
    public boolean tryStartTask(Long taskId, String workerId) {
        return false;
    }

    @Override
    public void failTask(Long taskId, String workerId, String errorMessage) {

    }

    @Override
    public void finishAutoProcess(Long taskId, String workerId, String autoResult) {

    }

    @Override
    public void reviewTask(Long taskId, String reviewerId, boolean approved, String manualResult) {

    }


    private static String assembleBizKey(Long projectId, String bizType, String bizId, String sourceId) {
        return TASK_KEY + projectId.toString() + COLON + bizType + COLON + bizId + COLON + sourceId;
    }


    private static void assembleAuditParam(WorkflowTask workflowTask) {
        workflowTask.setCreatedAt(LocalDateTime.now());
        workflowTask.setUpdatedAt(LocalDateTime.now());
    }
}
