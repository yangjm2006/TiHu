package com.tihu.backend.common;

/**
 * 自定义业务异常
 */
public class ApiException extends RuntimeException {
    private int code;
    private String message;
    private Object data;

    public ApiException(String message) {
        this(400, message);
    }

    public ApiException(int code, String message) {
        this(code, message, null);
    }

    public ApiException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

