package com.practice.workflow.common.enums;

/**
 * 业务错误码枚举
 * 命名规则：模块_场景_原因
 * 码段约定：
 *   1000~1999  通用参数类
 *   2000~2999  任务生命周期类
 */
public enum BizErrorCode {

    // -------- 通用 --------
    PARAM_INVALID(1000, "参数不合法"),
    PARAM_MISSING(1001, "缺少必要参数"),

    // -------- 任务 --------
    TASK_NOT_FOUND(2000, "任务不存在"),
    TASK_STATUS_ILLEGAL(2001, "任务状态不合法，无法执行该操作"),
    TASK_RETRY_EXCEEDED(2002, "任务重试次数已达上限"),
    TASK_WORKER_MISMATCH(2003, "操作者与任务持有者不匹配");

    private final int code;
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

