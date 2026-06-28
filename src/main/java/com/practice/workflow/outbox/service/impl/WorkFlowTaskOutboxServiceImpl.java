package com.practice.workflow.outbox.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.practice.workflow.messaging.message.WorkflowTaskEventType;
import com.practice.workflow.messaging.message.WorkflowTaskMessage;
import com.practice.workflow.outbox.domain.WorkflowTaskOutbox;
import com.practice.workflow.outbox.enums.OutboxStatus;
import com.practice.workflow.outbox.repository.WorkFlowTaskOutboxRepository;
import com.practice.workflow.outbox.service.WorkFlowTaskOutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>TODO</p>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/21 14:17
 */
@Service
@Slf4j
public class WorkFlowTaskOutboxServiceImpl implements WorkFlowTaskOutboxService {

    @Autowired
    private WorkFlowTaskOutboxRepository workFlowTaskOutboxRepository;


    public static final int MAX_RETRY_COUNT = 3;

    @Override
    public void createWorkFlowTaskOutbox(Long taskId, String bizKey) {
        WorkflowTaskOutbox workflowTaskOutbox = workFlowTaskOutboxRepository.findOutboxByTaskId(taskId);
        if (!Objects.isNull(workflowTaskOutbox)) {
            log.info("任务已存在");
            return;
        }
        workflowTaskOutbox = new WorkflowTaskOutbox();
        workflowTaskOutbox.setTaskId(taskId);
        workflowTaskOutbox.setBizKey(bizKey);
        workflowTaskOutbox.setEventType(WorkflowTaskEventType.AUTO_PROCESS_REQUESTED);
        workflowTaskOutbox.setMessageKey(bizKey + ":" + (UUID.randomUUID()));
        workflowTaskOutbox.setPayload(this.buildPayload(workflowTaskOutbox));
        workflowTaskOutbox.setStatus(OutboxStatus.PENDING);
        workflowTaskOutbox.setRetryCount(0);
        workflowTaskOutbox.setMaxRetry(MAX_RETRY_COUNT);
        workflowTaskOutbox.setCreatedAt(LocalDateTime.now());
        workflowTaskOutbox.setUpdatedAt(LocalDateTime.now());
        workFlowTaskOutboxRepository.insertWorkFlowTaskOutbox(workflowTaskOutbox);
    }

    private String buildPayload(WorkflowTaskOutbox workflowTaskOutbox) {
        WorkflowTaskMessage covert = new WorkflowTaskMessage();
        covert.setTaskId(workflowTaskOutbox.getTaskId());
        covert.setBizKey(workflowTaskOutbox.getBizKey());
        covert.setEventType(WorkflowTaskEventType.AUTO_PROCESS_REQUESTED);
        covert.setCreatedAt(LocalDateTime.now());
        covert.setTraceId(UUID.randomUUID().toString());
        return JSONObject.toJSONString(covert);
    }
}
