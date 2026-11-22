package com.company.platform.response;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiResponseTest {

    @Test
    void ok结构完整() {
        ApiResponse<String> r = ApiResponse.ok("data", "t-1");
        assertEquals("OK", r.getCode());
        assertEquals("OK", r.getMessage());
        assertEquals("data", r.getData());
        assertEquals("t-1", r.getTraceId());
    }

    @Test
    void fail带明细() {
        ApiResponse<Void> r = ApiResponse.fail("RF_VALID_001", "参数错", "t-2", Map.of("field", "scheme"));
        assertEquals("RF_VALID_001", r.getCode());
        assertEquals("t-2", r.getTraceId());
        assertEquals("scheme", r.getDetails().get("field"));
    }

    @Test
    void pageResponse防御性拷贝() {
        PageResponse<String> p = PageResponse.of(List.of("a"), 1, 1, 10);
        assertEquals(1, p.items().size());
        assertEquals(1, p.total());
    }
}
