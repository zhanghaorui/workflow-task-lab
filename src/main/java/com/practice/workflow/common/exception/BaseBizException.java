package com.practice.workflow.common.exception;

public class BaseBizException extends RuntimeException {

    private final int errorCode;


    public int getErrorCode() {
        return errorCode;
    }

    public BaseBizException(int code, String message) {
        super(message);
        this.errorCode = code;
    }
}
