package com.practice.workflow.common.exception;

import com.practice.workflow.common.enums.BizErrorCode;

/**
 * 业务异常基类
 *
 * <p>所有业务异常均应继承此类，而非直接继承 {@link RuntimeException}。
 * 通过 {@link BizErrorCode} 枚举统一管理错误码，禁止在构造时传入裸数字。
 *
 * <p>推荐用法（优先使用子类的静态工厂方法）：
 * <pre>
 *   throw WorkflowTaskException.notFound(taskId);
 * </pre>
 *
 * <p>若暂无对应子类，可直接使用：
 * <pre>
 *   throw new BaseBizException(BizErrorCode.PARAM_INVALID);
 *   throw new BaseBizException(BizErrorCode.PARAM_INVALID, "projectId 不能为空");
 * </pre>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/14
 */
public class BaseBizException extends RuntimeException {

    /** 业务错误码，来自 {@link BizErrorCode}，对外统一暴露 */
    private final int errorCode;

    /**
     * 使用枚举默认 message 构造异常。
     *
     * @param errorCode 错误码枚举
     */
    public BaseBizException(BizErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode.getCode();
    }

    /**
     * 使用枚举 code + 自定义 message 构造异常。
     * 适用于需要在 message 中拼接动态信息（如 ID、状态值）的场景。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义异常描述
     */
    public BaseBizException(BizErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode.getCode();
    }

    /**
     * 获取业务错误码。
     * 通常由全局异常处理器读取后写入统一响应体。
     *
     * @return 错误码
     */
    public int getErrorCode() {
        return errorCode;
    }
}
