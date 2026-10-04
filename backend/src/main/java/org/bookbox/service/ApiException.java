package org.bookbox.service;

/** 业务异常：携带 HTTP 状态码，由 ExceptionMapper 转为 JSON 响应 */
public class ApiException extends RuntimeException {

    public final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, message);
    }
}
