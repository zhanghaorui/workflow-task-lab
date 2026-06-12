package com.practice.workflow.service.impl;

import com.practice.workflow.domain.WorkflowTask;
import com.practice.workflow.service.WorkflowTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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


    /**
     * 根据 projectId + bizType + bizId + sourceId 生成 bizKey
     * 如果 bizKey 已存在，返回已有任务
     * 如果不存在，创建新任务
     * 初始状态 WAITING
     * retryCount = 0
     * maxRetry = 3
     * @param projectId
     * @param bizType
     * @param bizId
     * @param sourceId
     * @return
     */
    @Override
    public WorkflowTask createTask(Long projectId, String bizType, String bizId, String sourceId) {

        return null;
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
}
