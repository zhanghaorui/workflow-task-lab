package com.practice.workflow.outbox.service;

public interface WorkFlowTaskOutboxService {

    void createWorkFlowTaskOutbox(Long taskId, String bizKey);

}
