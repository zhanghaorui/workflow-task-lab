package com.practice.workflow.common.exception;

import com.practice.workflow.common.enums.BizErrorCode;

/**
 * 工作流任务业务异常
 * <p>
 * 每个静态工厂方法都绑定了固定的错误码，调用方只需关注"发生了什么"，无需关心 code。
 */
public class WorkflowTaskException extends BaseBizException {


    private WorkflowTaskException(BizErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    // -------- 静态工厂方法，错误码在这里固定 --------

    /**
     * 通用工厂方法，适用于没有预定义场景的情况
     */
    public static WorkflowTaskException of(BizErrorCode errorCode, String message) {
        return new WorkflowTaskException(errorCode, message);
    }

    /**
     * 通用工厂方法，使用枚举默认 message
     */
    public static WorkflowTaskException of(BizErrorCode errorCode) {
        return new WorkflowTaskException(errorCode, errorCode.getDefaultMessage());
    }


    public static WorkflowTaskException notFound(Long taskId) {
        return new WorkflowTaskException(BizErrorCode.TASK_NOT_FOUND,
                "任务[" + taskId + "]不存在");
    }

    public static WorkflowTaskException statusIllegal(Long taskId, String currentStatus) {
        return new WorkflowTaskException(BizErrorCode.TASK_STATUS_ILLEGAL,
                "任务[" + taskId + "]当前状态[" + currentStatus + "]不允许该操作");
    }

    public static WorkflowTaskException retryExceeded(Long taskId) {
        return new WorkflowTaskException(BizErrorCode.TASK_RETRY_EXCEEDED,
                "任务[" + taskId + "]重试次数已达上限");
    }

    public static WorkflowTaskException workerMismatch(Long taskId) {
        return new WorkflowTaskException(BizErrorCode.TASK_WORKER_MISMATCH,
                "任务[" + taskId + "]操作者与持有者不匹配");
    }
}

