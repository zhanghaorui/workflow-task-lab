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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>TODO</p>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/14 19:33
 */


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
        // 重新查询数据库获取最新状态
        WorkflowTask startedTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
        assertEquals(WorkflowTaskStatus.PROCESSING, startedTask.getStatus());
        assertEquals(workerId, startedTask.getWorkerId());

        return startedTask;
    }

    private WorkflowTask createStartedAndAutoFinishedTask(String workerId) {
        WorkflowTask task = createAndStartTask(workerId);

        assertDoesNotThrow(() ->
                workflowTaskService.finishAutoProcess(task.getId(), workerId, "auto-result")
        );

        // 重新查询数据库获取最新状态
        WorkflowTask finishedTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
        assertEquals(WorkflowTaskStatus.WAIT_MANUAL_REVIEW, finishedTask.getStatus());
        assertEquals("auto-result", finishedTask.getAutoResult());

        return finishedTask;
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
            // 重新查询数据库获取最新状态
            WorkflowTask startedTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.PROCESSING, startedTask.getStatus());
            assertEquals("worker-1", startedTask.getWorkerId());
            assertNotNull(startedTask.getStartedAt());
            assertNotNull(startedTask.getUpdatedAt());
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
            WorkflowTask task = createAndStartTask("worker-1");
            // 通过 failTask 让任务失败达到 maxRetry，状态变为 FAILED
            workflowTaskService.failTask(task.getId(), "worker-1", "error1");
            assertTrue(workflowTaskService.tryStartTask(task.getId(), "worker-1"));
            workflowTaskService.failTask(task.getId(), "worker-1", "error2");
            assertTrue(workflowTaskService.tryStartTask(task.getId(), "worker-1"));
            workflowTaskService.failTask(task.getId(), "worker-1", "error3");
            
            // 此时任务状态应为 FAILED
            WorkflowTask failedTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.FAILED, failedTask.getStatus());

            // 再次尝试启动，应返回 false
            boolean started = workflowTaskService.tryStartTask(failedTask.getId(), "worker-2");
            assertFalse(started);
        }

        @Test
        void shouldReturnFalseWhenRetryExceeded() {
            WorkflowTask task = createAndStartTask("worker-1");
            // 通过 failTask 让任务失败达到 maxRetry
            workflowTaskService.failTask(task.getId(), "worker-1", "error1");
            assertTrue(workflowTaskService.tryStartTask(task.getId(), "worker-1"));
            workflowTaskService.failTask(task.getId(), "worker-1", "error2");
            assertTrue(workflowTaskService.tryStartTask(task.getId(), "worker-1"));
            workflowTaskService.failTask(task.getId(), "worker-1", "error3");
            
            // 此时任务状态应为 FAILED
            WorkflowTask failedTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.FAILED, failedTask.getStatus());

            // 再次尝试启动，应返回 false
            boolean started = workflowTaskService.tryStartTask(failedTask.getId(), "worker-2");
            assertFalse(started);
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

            // 重新查询数据库获取最新状态
            WorkflowTask finishedTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.WAIT_MANUAL_REVIEW, finishedTask.getStatus());
            assertEquals("auto-result", finishedTask.getAutoResult());

            assertNull(finishedTask.getFinalResult());
            assertNull(finishedTask.getManualResult());

            // 这里是关键：
            // 自动处理完成不代表任务完成，所以 finishedAt 应该还是 null。
            assertNull(finishedTask.getFinishedAt());
        }

        @Test
        void shouldThrowWhenWorkerMismatch() {
            WorkflowTask task = createAndStartTask("worker-1");

            WorkflowTaskException exception = assertThrows(
                    WorkflowTaskException.class,
                    () -> workflowTaskService.finishAutoProcess(task.getId(), "worker-2", "auto-result")
            );
            assertEquals(BizErrorCode.TASK_WORKER_MISMATCH.getCode(), exception.getErrorCode());
            // 重新查询数据库获取最新状态
            WorkflowTask currentTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.PROCESSING, currentTask.getStatus());
            assertNull(currentTask.getAutoResult());
            assertNull(currentTask.getFinalResult());
        }

        @Test
        void shouldThrowWhenTaskIsNotProcessing() {
            WorkflowTask task = createNewTask();

            WorkflowTaskException exception = assertThrows(
                    WorkflowTaskException.class,
                    () -> workflowTaskService.finishAutoProcess(task.getId(), "worker-1", "auto-result")
            );
            assertEquals(BizErrorCode.TASK_STATUS_ILLEGAL.getCode(), exception.getErrorCode());

            // 重新查询数据库获取最新状态
            WorkflowTask currentTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.WAITING, currentTask.getStatus());
            assertNull(currentTask.getAutoResult());
            assertNull(currentTask.getFinalResult());
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

            // 重新查询数据库获取最新状态
            WorkflowTask reviewedTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.REVIEW_CONFIRMED, reviewedTask.getStatus());
            assertEquals("manual-confirmed-result", reviewedTask.getManualResult());
            assertEquals("manual-confirmed-result", reviewedTask.getFinalResult());

            assertNotNull(reviewedTask.getFinishedAt());
            assertNotNull(reviewedTask.getUpdatedAt());
        }

        @Test
        void shouldRejectTaskAndSetFinalResultFromManualResult() {
            WorkflowTask task = createStartedAndAutoFinishedTask("worker-1");

            assertDoesNotThrow(() ->
                    workflowTaskService.reviewTask(task.getId(), "reviewer-1", false, "manual-rejected-result")
            );

            // 重新查询数据库获取最新状态
            WorkflowTask reviewedTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.REVIEW_REJECTED, reviewedTask.getStatus());
            assertEquals("manual-rejected-result", reviewedTask.getManualResult());
            assertEquals("manual-rejected-result", reviewedTask.getFinalResult());

            assertNotNull(reviewedTask.getFinishedAt());
            assertNotNull(reviewedTask.getUpdatedAt());
        }

        @Test
        void shouldThrowWhenTaskIsNotWaitManualReview() {
            WorkflowTask task = createAndStartTask("worker-1");

            WorkflowTaskException exception = assertThrows(
                    WorkflowTaskException.class,
                    () -> workflowTaskService.reviewTask(task.getId(), "reviewer-1", true, "manual-result")
            );
            assertEquals(BizErrorCode.TASK_STATUS_ILLEGAL.getCode(), exception.getErrorCode());

            // 重新查询数据库获取最新状态
            WorkflowTask currentTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.PROCESSING, currentTask.getStatus());
            assertNull(currentTask.getManualResult());
            assertNull(currentTask.getFinalResult());
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

            // 重新查询数据库获取最新状态
            WorkflowTask failedTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.AUTO_PROCESS_FAILED, failedTask.getStatus());
            assertEquals(1, failedTask.getRetryCount());
            assertEquals("first error", failedTask.getErrorMessage());

            assertNull(failedTask.getFinishedAt());
        }

        @Test
        void shouldMoveToFailedWhenRetryExceeded() {
            WorkflowTask task = createAndStartTask("worker-1");

            workflowTaskService.failTask(task.getId(), "worker-1", "first error");
            WorkflowTask task1 = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.AUTO_PROCESS_FAILED, task1.getStatus());

            assertTrue(workflowTaskService.tryStartTask(task.getId(), "worker-1"));
            workflowTaskService.failTask(task.getId(), "worker-1", "second error");
            WorkflowTask task2 = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.AUTO_PROCESS_FAILED, task2.getStatus());

            assertTrue(workflowTaskService.tryStartTask(task.getId(), "worker-1"));

            // 第三次失败达到 maxRetry，进入 FAILED。
            assertDoesNotThrow(() ->
                    workflowTaskService.failTask(task.getId(), "worker-1", "third error")
            );

            // 重新查询数据库获取最新状态
            WorkflowTask finalTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.FAILED, finalTask.getStatus());
            assertEquals(3, finalTask.getRetryCount());
            assertEquals("third error", finalTask.getErrorMessage());
            assertNotNull(finalTask.getFinishedAt());
        }

        @Test
        void shouldThrowWhenWorkerMismatch() {
            WorkflowTask task = createAndStartTask("worker-1");

            WorkflowTaskException exception = assertThrows(
                    WorkflowTaskException.class,
                    () -> workflowTaskService.failTask(task.getId(), "worker-2", "error")
            );
            assertEquals(BizErrorCode.TASK_WORKER_MISMATCH.getCode(), exception.getErrorCode());

            // 重新查询数据库获取最新状态
            WorkflowTask currentTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.PROCESSING, currentTask.getStatus());
            assertEquals(0, currentTask.getRetryCount());
            assertNull(currentTask.getErrorMessage());
        }

        @Test
        void shouldThrowWhenTaskIsNotProcessing() {
            WorkflowTask task = createNewTask();

            WorkflowTaskException exception = assertThrows(
                    WorkflowTaskException.class,
                    () -> workflowTaskService.failTask(task.getId(), "worker-1", "error")
            );
            assertEquals(BizErrorCode.TASK_STATUS_ILLEGAL.getCode(), exception.getErrorCode());

            // 重新查询数据库获取最新状态
            WorkflowTask currentTask = workflowTaskService.createTask(PROJECT_ID, BIZ_TYPE, task.getBizId(), SOURCE_ID);
            assertEquals(WorkflowTaskStatus.WAITING, currentTask.getStatus());
            assertEquals(0, currentTask.getRetryCount());
            assertNull(currentTask.getErrorMessage());
        }
    }
}