package com.company.platform.exception;

import com.company.platform.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void 业务异常映射错误码与状态() {
        ResponseEntity<ApiResponse<Void>> r = handler.handleBusiness(
                new BusinessException(StandardErrorCode.CONFLICT_001, "冲突", Map.of("k", "v")), request);
        assertEquals(409, r.getStatusCode().value());
        assertEquals("CONFLICT_001", r.getBody().getCode());
        assertEquals("v", r.getBody().getDetails().get("k"));
        assertNotNull(r.getBody().getTraceId());
    }

    @Test
    void 业务异常透传请求traceId() {
        request.setAttribute("traceId", "trace-from-gateway");
        ResponseEntity<ApiResponse<Void>> r = handler.handleBusiness(
                new BusinessException(StandardErrorCode.VALID_001, "参数错"), request);
        assertEquals("trace-from-gateway", r.getBody().getTraceId());
    }

    @Test
    void 入参非法归4xx不落500() {
        ResponseEntity<ApiResponse<Void>> r = handler.handleIllegalArgument(
                new IllegalArgumentException("金额非法"), request);
        assertEquals(400, r.getStatusCode().value());
        assertEquals("VALID_001", r.getBody().getCode());
    }

    @Test
    void 未知异常归500兜底码() {
        ResponseEntity<ApiResponse<Void>> r = handler.handleUnknown(
                new RuntimeException("boom"), request);
        assertEquals(500, r.getStatusCode().value());
        assertEquals("SYS_001", r.getBody().getCode());
    }
}
