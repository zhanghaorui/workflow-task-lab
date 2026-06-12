package com.practice.workflow.common.enums;

/**
 * <p>TODO</p>
 *
 * @author 张浩锐
 * @version V1.0.0
 * @date 2026/6/8 22:32
 */

public enum WorkflowTaskStatus {
    WAITING,
    PROCESSING,
    AUTO_PROCESS_SUCCESS,
    AUTO_PROCESS_FAILED,
    WAIT_MANUAL_REVIEW,
    REVIEW_CONFIRMED,
    REVIEW_REJECTED,
    ARCHIVED,
    FAILED
}
