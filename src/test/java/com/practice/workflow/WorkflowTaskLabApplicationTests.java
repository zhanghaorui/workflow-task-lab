package com.practice.workflow.service;

import com.practice.workflow.common.enums.BizErrorCode;
import com.practice.workflow.common.enums.WorkflowTaskStatus;
import com.practice.workflow.common.exception.WorkflowTaskException;
import com.practice.workflow.domain.WorkflowTask;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WorkflowTaskServiceTest {

    @Autowired
    private WorkflowTaskService workflowTaskService;

    private static final Long PROJECT_ID = 100L;
    private static final String BIZ_TYPE = "QC";
    private static final String SOURCE_ID = "mq";

    private WorkflowTask createNewTask() {
        String bizId = "biz-" + UUID.randomUUID();
        return workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, bizId, SOURCE_ID);
    }

    private WorkflowTask createAndStartTask(String workerId) {
        WorkflowTask task = createNewTask();

        boolean started = workflowTaskService.tryStartTask(task.getId(), workerId);

        assertTrue(started);
        assertEquals(WorkflowTaskStatus.PROCESSING, task.getStatus());
        assertEquals(workerId, task.getWorkerId());

        return task;
    }

    private WorkflowTask createStartedAndAutoFinishedTask(String workerId) {
        WorkflowTask task = createAndStartTask(workerId);

        assertDoesNotThrow(() ->
                workflowTaskService.finishAutoProcess(task.getId(), workerId, "auto-result")
        );

        assertEquals(WorkflowTaskStatus.WAIT_MANUAL_REVIEW, task.getStatus());
        assertEquals("auto-result", task.getAutoResult());

        return task;
    }

    @Nested
    class CreateTaskTests {


        @Test
        void shouldThrowInvalidParam() {
            WorkflowTaskException exception = assertThrows(WorkflowTaskException.class, () -> {
                workflowTaskService.createTask(null, "", "", "");
            });
            assertEquals(BizErrorCode.PARAM_INVALID.getCode(), exception.getErrorCode());

        }

        @Test
        void shouldCreateWaitingTask() {
            String bizId = "biz-" + UUID.randomUUID();

            WorkflowTask task = workflowTaskService.createTask(
                    PROJECT_ID,
                    BIZ_TYPE,
                    bizId,
                    SOURCE_ID
            );

            assertNotNull(task);
            assertNotNull(task.getId());

            assertEquals(PROJECT_ID, task.getProjectId());
            assertEquals(BIZ_TYPE, task.getBizType());
            assertEquals(bizId, task.getBizId());
            assertEquals(SOURCE_ID, task.getSourceId());

            assertEquals("task:" + PROJECT_ID + ":" + BIZ_TYPE + ":" + bizId + ":" + SOURCE_ID,
                    task.getBizKey());

            assertEquals(WorkflowTaskStatus.WAITING, task.getStatus());
            assertEquals(0, task.getRetryCount());
            assertEquals(3, task.getMaxRetry());

            assertNotNull(task.getCreatedAt());
            assertNotNull(task.getUpdatedAt());

            assertNull(task.getStartedAt());
            assertNull(task.getFinishedAt());
            assertNull(task.getWorkerId());
            assertNull(task.getAutoResult());
            assertNull(task.getManualResult());
            assertNull(task.getFinalResult());
            assertNull(task.getErrorMessage());
        }

        @Test
        void shouldReturnSameTaskWhenSameBizKeyCreatedRepeatedly() {
            String bizId = "biz-" + UUID.randomUUID();

            WorkflowTask firstTask = workflowTaskService.createTask(
                    PROJECT_ID,
                    BIZ_TYPE,
                    bizId,
                    SOURCE_ID
            );

            assertNotNull(firstTask);
            assertNotNull(firstTask.getId());

            Set<Long> taskIds = new HashSet<>();

            for (int i = 0; i < 100; i++) {
                WorkflowTask task = workflowTaskService.createTask(
                        PROJECT_ID,
                        BIZ_TYPE,
                        bizId,
                        SOURCE_ID
                );

                assertEquals(firstTask.getId(), task.getId());
                assertEquals(firstTask.getBizKey(), task.getBizKey());

                taskIds.add(task.getId());
            }

            assertEquals(1, taskIds.size());
        }
    }

    @Nested
    class TryStartTaskTests {

        @Test
        void shouldStartWaitingTask() {
            WorkflowTask task = createNewTask();

            boolean started = workflowTaskService.tryStartTask(task.getId(), "worker-1");

            assertTrue(started);
            assertEquals(WorkflowTaskStatus.PROCESSING, task.getStatus());
            assertEquals("worker-1", task.getWorkerId());
            assertNotNull(task.getStartedAt());
            assertNotNull(task.getUpdatedAt());
        }

        @Test
        void shouldReturnFalseWhenAlreadyProcessingAndNotOverrideWorker() {
            WorkflowTask task = createAndStartTask("worker-1");

            boolean secondStart = workflowTaskService.tryStartTask(task.getId(), "worker-2");

            assertFalse(secondStart);
            assertEquals(WorkflowTaskStatus.PROCESSING, task.getStatus());
            assertEquals("worker-1", task.getWorkerId());
        }

        @Test
        void shouldReturnFalseWhenTaskStatusIsFailed() {
            WorkflowTask task = createNewTask();
            task.setStatus(WorkflowTaskStatus.FAILED);

            boolean started = workflowTaskService.tryStartTask(task.getId(), "worker-1");

            assertFalse(started);
            assertEquals(WorkflowTaskStatus.FAILED, task.getStatus());
            assertNull(task.getWorkerId());
        }

        @Test
        void shouldReturnFalseWhenRetryExceeded() {
            WorkflowTask task = createNewTask();
            task.setStatus(WorkflowTaskStatus.AUTO_PROCESS_FAILED);
            task.setRetryCount(task.getMaxRetry());

            boolean started = workflowTaskService.tryStartTask(task.getId(), "worker-1");

            assertFalse(started);
            assertEquals(WorkflowTaskStatus.AUTO_PROCESS_FAILED, task.getStatus());
            assertNull(task.getWorkerId());
        }
    }

    @Nested
    class FinishAutoProcessTests {

        @Test
        void shouldMoveProcessingTaskToWaitManualReview() {
            WorkflowTask task = createAndStartTask("worker-1");

            assertDoesNotThrow(() ->
                    workflowTaskService.finishAutoProcess(task.getId(), "worker-1", "auto-result")
            );

            assertEquals(WorkflowTaskStatus.WAIT_MANUAL_REVIEW, task.getStatus());
            assertEquals("auto-result", task.getAutoResult());

            assertNull(task.getFinalResult());
            assertNull(task.getManualResult());

            // 这里是关键：
            // 自动处理完成不代表任务完成，所以 finishedAt 应该还是 null。
            assertNull(task.getFinishedAt());
        }

        @Test
        void shouldThrowWhenWorkerMismatch() {
            WorkflowTask task = createAndStartTask("worker-1");

            WorkflowTaskException exception = assertThrows(
                    WorkflowTaskException.class,
                    () -> workflowTaskService.finishAutoProcess(task.getId(), "worker-2", "auto-result")
            );
            assertEquals(BizErrorCode.TASK_WORKER_MISMATCH.getCode(), exception.getErrorCode());
            assertEquals(WorkflowTaskStatus.PROCESSING, task.getStatus());
            assertNull(task.getAutoResult());
            assertNull(task.getFinalResult());
        }

        @Test
        void shouldThrowWhenTaskIsNotProcessing() {
            WorkflowTask task = createNewTask();

            assertThrows(
                    WorkflowTaskException.class,
                    () -> workflowTaskService.finishAutoProcess(task.getId(), "worker-1", "auto-result")
            );

            assertEquals(WorkflowTaskStatus.WAITING, task.getStatus());
            assertNull(task.getAutoResult());
            assertNull(task.getFinalResult());
        }
    }

    @Nested
    class ReviewTaskTests {

        @Test
        void shouldConfirmTaskAndSetFinalResultFromManualResult() {
            WorkflowTask task = createStartedAndAutoFinishedTask("worker-1");

            assertDoesNotThrow(() ->
                    workflowTaskService.reviewTask(task.getId(), "reviewer-1", true, "manual-confirmed-result")
            );

            assertEquals(WorkflowTaskStatus.REVIEW_CONFIRMED, task.getStatus());
            assertEquals("manual-confirmed-result", task.getManualResult());
            assertEquals("manual-confirmed-result", task.getFinalResult());

            assertNotNull(task.getFinishedAt());
            assertNotNull(task.getUpdatedAt());
        }

        @Test
        void shouldRejectTaskAndSetFinalResultFromManualResult() {
            WorkflowTask task = createStartedAndAutoFinishedTask("worker-1");

            assertDoesNotThrow(() ->
                    workflowTaskService.reviewTask(task.getId(), "reviewer-1", false, "manual-rejected-result")
            );

            assertEquals(WorkflowTaskStatus.REVIEW_REJECTED, task.getStatus());
            assertEquals("manual-rejected-result", task.getManualResult());
            assertEquals("manual-rejected-result", task.getFinalResult());

            assertNotNull(task.getFinishedAt());
            assertNotNull(task.getUpdatedAt());
        }

        @Test
        void shouldThrowWhenTaskIsNotWaitManualReview() {
            WorkflowTask task = createAndStartTask("worker-1");

            assertThrows(
                    WorkflowTaskException.class,
                    () -> workflowTaskService.reviewTask(task.getId(), "reviewer-1", true, "manual-result")
            );

            assertEquals(WorkflowTaskStatus.PROCESSING, task.getStatus());
            assertNull(task.getManualResult());
            assertNull(task.getFinalResult());
        }
    }

    @Nested
    class FailTaskTests {

        @Test
        void shouldMoveToAutoProcessFailedBeforeRetryExceeded() {
            WorkflowTask task = createAndStartTask("worker-1");

            assertDoesNotThrow(() ->
                    workflowTaskService.failTask(task.getId(), "worker-1", "first error")
            );

            assertEquals(WorkflowTaskStatus.AUTO_PROCESS_FAILED, task.getStatus());
            assertEquals(1, task.getRetryCount());
            assertEquals("first error", task.getErrorMessage());

            assertNull(task.getFinishedAt());
        }

        @Test
        void shouldMoveToFailedWhenRetryExceeded() {
            WorkflowTask task = createAndStartTask("worker-1");

            assertDoesNotThrow(() ->
                    workflowTaskService.failTask(task.getId(), "worker-1", "first error")
            );
            assertEquals(WorkflowTaskStatus.AUTO_PROCESS_FAILED, task.getStatus());

            assertTrue(workflowTaskService.tryStartTask(task.getId(), "worker-1"));
            assertDoesNotThrow(() ->
                    workflowTaskService.failTask(task.getId(), "worker-1", "second error")
            );
            assertEquals(WorkflowTaskStatus.AUTO_PROCESS_FAILED, task.getStatus());

            assertTrue(workflowTaskService.tryStartTask(task.getId(), "worker-1"));

            // 第三次失败达到 maxRetry，进入 FAILED。
            // 这里根据你的设计，如果你认为达到上限应该抛异常，
            // 就把 assertDoesNotThrow 改成 assertThrows。
            assertDoesNotThrow(() ->
                    workflowTaskService.failTask(task.getId(), "worker-1", "third error")
            );

            assertEquals(WorkflowTaskStatus.FAILED, task.getStatus());
            assertEquals(3, task.getRetryCount());
            assertEquals("third error", task.getErrorMessage());
            assertNotNull(task.getFinishedAt());
        }

        @Test
        void shouldThrowWhenWorkerMismatch() {
            WorkflowTask task = createAndStartTask("worker-1");

            assertThrows(
                    WorkflowTaskException.class,
                    () -> workflowTaskService.failTask(task.getId(), "worker-2", "error")
            );

            assertEquals(WorkflowTaskStatus.PROCESSING, task.getStatus());
            assertEquals(0, task.getRetryCount());
            assertNull(task.getErrorMessage());
        }

        @Test
        void shouldThrowWhenTaskIsNotProcessing() {
            WorkflowTask task = createNewTask();

            assertThrows(
                    WorkflowTaskException.class,
                    () -> workflowTaskService.failTask(task.getId(), "worker-1", "error")
            );

            assertEquals(WorkflowTaskStatus.WAITING, task.getStatus());
            assertEquals(0, task.getRetryCount());
            assertNull(task.getErrorMessage());
        }
    }
}