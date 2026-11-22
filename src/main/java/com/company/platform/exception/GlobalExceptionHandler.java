package com.company.platform.exception;

import com.company.platform.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理骨架（机制层）：业务异常映射错误码，入参非法归 400，未知异常归 500。
 *
 * <p>与 floorplan 现网版本同构；错误码来源是 {@link StandardErrorCode}（可用子类覆盖）。
 * traceId 取不到请求属性时自行生成，保证响应永远带 traceId。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorCode validCode;
    private final ErrorCode sysCode;

    public GlobalExceptionHandler() {
        this(StandardErrorCode.VALID_001, StandardErrorCode.SYS_001);
    }

    public GlobalExceptionHandler(ErrorCode validCode, ErrorCode sysCode) {
        this.validCode = validCode;
        this.sysCode = sysCode;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest request) {
        String traceId = traceId(request);
        log.warn("业务异常 code={} traceId={} msg={}", ex.getErrorCode().getCode(), traceId, ex.getMessage());
        ApiResponse<Void> body = ApiResponse.fail(
                ex.getErrorCode().getCode(), ex.getMessage(), traceId, ex.getDetails());
        return ResponseEntity.status(HttpStatus.valueOf(ex.getErrorCode().getHttpStatus())).body(body);
    }

    /**
     * 入参校验异常：归 4xx，不落到 {@link #handleUnknown} 冒泡成 500——
     * 客户端传错参数不是系统故障。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex,
                                                                   HttpServletRequest request) {
        String traceId = traceId(request);
        log.warn("入参校验失败 traceId={} msg={}", traceId, ex.getMessage());
        ApiResponse<Void> body = ApiResponse.fail(validCode.getCode(), ex.getMessage(), traceId, null);
        return ResponseEntity.status(HttpStatus.valueOf(validCode.getHttpStatus())).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex, HttpServletRequest request) {
        String traceId = java.util.UUID.randomUUID().toString();
        log.error("系统异常 traceId={}", traceId, ex);
        ApiResponse<Void> body = ApiResponse.fail(
                sysCode.getCode(), sysCode.getMessage() + "：" + ex.getMessage(), traceId, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String traceId(HttpServletRequest request) {
        String traceId = String.valueOf(request.getAttribute("traceId"));
        return "null".equals(traceId) ? java.util.UUID.randomUUID().toString() : traceId;
    }
}
