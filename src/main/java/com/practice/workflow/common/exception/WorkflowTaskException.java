package com.practice.workflow.common.exception;

import com.practice.workflow.common.enums.BizErrorCode;

/**
 * 工作流任务业务异常
 *
 * <p>构造器为私有，外部只能通过静态工厂方法创建实例，确保每种场景绑定固定的错误码，
 * 调用方无需关心具体 code 数字。
 *
 * <p>预定义场景方法：
 * <ul>
 *   <li>{@link #notFound(Long)}         — 任务不存在</li>
 *   <li>{@link #statusIllegal(Long, String)} — 任务状态不合法</li>
 *   <li>{@link #retryExceeded(Long)}    — 重试次数超限</li>
 *   <li>{@link #workerMismatch(Long)}   — 操作者不匹配</li>
 * </ul>
 *
 * <p>若无对应预定义方法，可使用通用工厂：
 * <pre>
 *   throw WorkflowTaskException.of(BizErrorCode.TASK_STATUS_ILLEGAL, "自定义描述");
 * </pre>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/14
 */
public class WorkflowTaskException extends BaseBizException {

    /**
     * 私有构造器，强制通过静态工厂方法创建，保证错误码不被外部随意指定。
     */
    private WorkflowTaskException(BizErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    // -------- 通用工厂方法 --------

    /**
     * 通用工厂方法，使用枚举 code + 自定义 message。
     * 适用于暂无对应预定义场景的情况。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义描述
     */
    public static WorkflowTaskException of(BizErrorCode errorCode, String message) {
        return new WorkflowTaskException(errorCode, message);
    }

    /**
     * 通用工厂方法，使用枚举默认 message。
     *
     * @param errorCode 错误码枚举
     */
    public static WorkflowTaskException of(BizErrorCode errorCode) {
        return new WorkflowTaskException(errorCode, errorCode.getDefaultMessage());
    }

    // -------- 预定义场景工厂方法 --------

    /**
     * 任务不存在。
     * 对应错误码：{@link BizErrorCode#TASK_NOT_FOUND}
     *
     * @param taskId 任务 ID
     */
    public static WorkflowTaskException notFound(Long taskId) {
        return new WorkflowTaskException(BizErrorCode.TASK_NOT_FOUND,
                "任务[" + taskId + "]不存在");
    }

    /**
     * 任务当前状态不允许执行该操作。
     * 对应错误码：{@link BizErrorCode#TASK_STATUS_ILLEGAL}
     *
     * @param taskId        任务 ID
     * @param currentStatus 任务当前状态名称
     */
    public static WorkflowTaskException statusIllegal(Long taskId, String currentStatus) {
        return new WorkflowTaskException(BizErrorCode.TASK_STATUS_ILLEGAL,
                "任务[" + taskId + "]当前状态[" + currentStatus + "]不允许该操作");
    }

    /**
     * 任务重试次数已达上限，不可再次重试。
     * 对应错误码：{@link BizErrorCode#TASK_RETRY_EXCEEDED}
     *
     * @param taskId 任务 ID
     */
    public static WorkflowTaskException retryExceeded(Long taskId) {
        return new WorkflowTaskException(BizErrorCode.TASK_RETRY_EXCEEDED,
                "任务[" + taskId + "]重试次数已达上限");
    }

    /**
     * 操作者 workerId 与任务当前持有者不一致。
     * 对应错误码：{@link BizErrorCode#TASK_WORKER_MISMATCH}
     *
     * @param taskId 任务 ID
     */
    public static WorkflowTaskException workerMismatch(Long taskId) {
        return new WorkflowTaskException(BizErrorCode.TASK_WORKER_MISMATCH,
                "任务[" + taskId + "]操作者与持有者不匹配");
    }
}
