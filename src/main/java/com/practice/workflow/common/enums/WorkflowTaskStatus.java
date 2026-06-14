package com.practice.workflow.common.enums;

/**
 * <p>TODO</p>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/8 22:32
 */

public enum WorkflowTaskStatus {

    /**
     * 未开始
     */
    WAITING,

    /**
     * 处理中
     */
    PROCESSING,

    @Deprecated
    AUTO_PROCESS_SUCCESS,

    /**
     * 自动处理失败
     */
    AUTO_PROCESS_FAILED,

    /**
     * 等待人工处理
     */
    WAIT_MANUAL_REVIEW,


    /**
     * 人工确认通过
     */
    REVIEW_CONFIRMED,


    /**
     * 人工拒绝
     */
    REVIEW_REJECTED,

    @Deprecated
    ARCHIVED,


    /**
     * 处理失败
     *
     */
    FAILED
}
