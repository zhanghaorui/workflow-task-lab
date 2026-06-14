package com.practice.workflow;

import com.practice.workflow.common.enums.WorkflowTaskStatus;
import com.practice.workflow.domain.WorkflowTask;
import com.practice.workflow.service.WorkflowTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WorkflowTaskLabApplicationTests {

    @Autowired
    private WorkflowTaskService workflowTaskService;

    @Test
    void createTask_shouldCreateWaitingTask() {
        Long projectId = 100L;
        String bizType = "QC";
        String sourceId = "mq";
        String bizId = "b-" + UUID.randomUUID();

        WorkflowTask task = workflowTaskService.createTask(projectId, bizType, bizId, sourceId);

        assertNotNull(task);
        assertNotNull(task.getId());

        assertEquals(projectId, task.getProjectId());
        assertEquals(bizType, task.getBizType());
        assertEquals(bizId, task.getBizId());
        assertEquals(sourceId, task.getSourceId());

        assertEquals("task:" + projectId + ":" + bizType + ":" + bizId + ":" + sourceId, task.getBizKey());
        assertEquals(WorkflowTaskStatus.WAITING, task.getStatus());

        assertEquals(0, task.getRetryCount());
        assertEquals(3, task.getMaxRetry());

        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());

        assertNull(task.getWorkerId());
        assertNull(task.getAutoResult());
        assertNull(task.getManualResult());
        assertNull(task.getFinalResult());
        assertNull(task.getErrorMessage());
    }

    @Test
    void createTask_shouldReturnSameTaskWhenSameBizKeyCreatedRepeatedly() {
        Long projectId = 100L;
        String bizType = "QC";
        String sourceId = "mq";
        String bizId = "b-" + UUID.randomUUID();

        WorkflowTask firstTask = workflowTaskService.createTask(projectId, bizType, bizId, sourceId);

        assertNotNull(firstTask);
        assertNotNull(firstTask.getId());

        Long firstTaskId = firstTask.getId();
        String firstBizKey = firstTask.getBizKey();

        Set<Long> taskIds = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            WorkflowTask task = workflowTaskService.createTask(projectId, bizType, bizId, sourceId);
            assertNotNull(task);
            assertEquals(firstTaskId, task.getId());
            assertEquals(firstBizKey, task.getBizKey());
            assertEquals(WorkflowTaskStatus.WAITING, task.getStatus());
            taskIds.add(task.getId());
        }

        assertEquals(1, taskIds.size());
    }
}