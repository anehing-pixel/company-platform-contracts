package com.company.platform.response;

import java.util.Map;

/**
 * 统一响应结构：{ code, message, data, traceId, details? }。
 *
 * <p>公司级共享内核（机制层）：JSON 结构与字段顺序与 floorplan 现网版本保持一致，
 * 任何使用方不得在本类上叠加业务语义。分页见 {@link PageResponse}。</p>
 */
public class ApiResponse<T> {

    private String code;
    private String message;
    private T data;
    private String traceId;
    private Map<String, Object> details;

    public ApiResponse() {
    }

    public static <T> ApiResponse<T> ok(T data, String traceId) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = "OK";
        r.message = "OK";
        r.data = data;
        r.traceId = traceId;
        return r;
    }

    public static <T> ApiResponse<T> fail(String code, String message, String traceId, Map<String, Object> details) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.message = message;
        r.traceId = traceId;
        r.details = details;
        return r;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
