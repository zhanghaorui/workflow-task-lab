package com.practice.workflow.service;

import com.practice.workflow.domain.WorkflowTask;

public interface WorkflowTaskService {

     WorkflowTask createTask(Long projectId, String bizType, String bizId, String sourceId);

     boolean tryStartTask(Long taskId, String workerId);

     void finishAutoProcess(Long taskId, String workerId, String autoResult);

     void failTask(Long taskId, String workerId, String errorMessage);

     void reviewTask(Long taskId, String reviewerId, boolean approved, String manualResult);

}
