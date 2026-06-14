package com.practice.workflow.common.enums;

/**
 * 业务错误码枚举
 *
 * <p>所有业务异常的错误码在此统一定义，避免魔法数字散落在代码中。
 *
 * <p>码段约定：
 * <pre>
 *   1000 ~ 1999  通用参数类
 *   2000 ~ 2999  工作流任务生命周期类
 * </pre>
 *
 * <p>使用方式：配合 {@link com.practice.workflow.common.exception.BaseBizException} 及其子类使用，
 * 不应在业务代码中直接使用原始 code 数字。
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/14
 */
public enum BizErrorCode {

    // -------- 通用参数类 1000 ~ 1999 --------

    /** 请求参数不合法，如格式错误、越界等 */
    PARAM_INVALID(1000, "参数不合法"),

    /** 缺少必要的请求参数 */
    PARAM_MISSING(1001, "缺少必要参数"),

    // -------- 工作流任务类 2000 ~ 2999 --------

    /** 根据 ID 或 bizKey 查找任务时，任务不存在 */
    TASK_NOT_FOUND(2000, "任务不存在"),

    /** 任务当前状态不允许执行该操作，如对 FAILED 状态的任务发起重试 */
    TASK_STATUS_ILLEGAL(2001, "任务状态不合法，无法执行该操作"),

    /** 任务重试次数已达到 maxRetry 上限，不可再次重试 */
    TASK_RETRY_EXCEEDED(2002, "任务重试次数已达上限"),

    /** 操作者 workerId 与任务当前持有者不一致，拒绝操作 */
    TASK_WORKER_MISMATCH(2003, "操作者与任务持有者不匹配");

    /** 错误码，对外暴露给调用方 / 前端 */
    private final int code;

    /** 默认错误描述，未指定自定义 message 时使用 */
    private final String defaultMessage;

    BizErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
