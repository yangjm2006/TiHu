package com.tihu.backend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果类
 */
@Data
public class Result implements Serializable {
    private static final long serialVersionUID = 1L;
    //  状态码
    private int code;
    //  提示信息message
    private String msg;
    //  返回的属性类型
    private Object data;

    /**
     * 自定义返回成功结果
     *
     * @param code
     * @param msg
     * @param data
     * @return
     */
    public static Result success(int code, String msg, Object data) {
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    /**
     * 直接返回成功结果
     *
     * @param data
     * @return
     */
    public static Result success(Object data) {
        return success(200, "操作成功", data);
    }

    /**
     * 不带结果直接返回成功
     *
     * @return
     */
    public static Result success() {
        return success(200, "操作成功", null);
    }

    /**
     * 自定义返回失败结果
     *
     * @param code
     * @param msg
     * @param data
     * @return
     */
    public static Result error(int code, String msg, Object data) {
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    /**
     * 带参数返回失败
     *
     * @param msg
     * @return
     */
    public static Result error(String msg) {
        return error(400, msg, null);
    }

    /**
     * 直接返回失败结果
     *
     * @param data
     * @return
     */
    public static Result error(Object data) {
        return error(400, "操作失败", data);
    }

    /**
     * 不带结果直接返回成功
     *
     * @return
     */
    public static Result error() {
        return error(400, "操作失败", null);
    }

}
