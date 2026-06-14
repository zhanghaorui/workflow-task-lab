package com.practice.workflow.common.exception;

import com.practice.workflow.common.enums.BizErrorCode;

/**
 * 业务异常基类
 * <p>
 * 推荐用法：
 *   throw new BaseBizException(BizErrorCode.TASK_NOT_FOUND);
 *   throw new BaseBizException(BizErrorCode.TASK_NOT_FOUND, "任务[" + taskId + "]不存在");
 */
public class BaseBizException extends RuntimeException {

    private final int errorCode;

    /**
     * 使用枚举默认 message
     */
    public BaseBizException(BizErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode.getCode();
    }

    /**
     * 使用枚举 code + 自定义 message（用于携带动态信息）
     */
    public BaseBizException(BizErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode.getCode();
    }

    public int getErrorCode() {
        return errorCode;
    }
}
