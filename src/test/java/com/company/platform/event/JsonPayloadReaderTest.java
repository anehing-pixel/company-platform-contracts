package com.company.platform.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 切断点 ① 的可执行证据：业务主键可声明，vin 不再是硬编码。 */
class JsonPayloadReaderTest {

    private static final String RF_PAYLOAD =
            "{\"applicationId\":\"AP-001\",\"traceId\":\"t-1\",\"amount\":123456}";
    private static final String FP_PAYLOAD =
            "{\"vin\":\"VIN-9\",\"traceId\":\"t-2\"}";

    @Test
    void rf用applicationId作业务主键通过校验() {
        JsonPayloadReader.requireBizKeyAndTraceId(RF_PAYLOAD, "applicationId");
    }

    @Test
    void floorplan同口径用vin作业务主键也通过() {
        JsonPayloadReader.requireBizKeyAndTraceId(FP_PAYLOAD, "vin");
    }

    @Test
    void 多业务主键全部声明才通过() {
        String p = "{\"vin\":\"V\",\"advanceId\":\"A\",\"traceId\":\"t\"}";
        JsonPayloadReader.requireBizKeysAndTraceId(p, "vin", "advanceId");
        assertThrows(IllegalArgumentException.class,
                () -> JsonPayloadReader.requireBizKeysAndTraceId(p, "vin", "advanceId", "paymentRef"));
    }

    @Test
    void 缺业务主键拒绝() {
        String p = "{\"traceId\":\"t-1\"}";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JsonPayloadReader.requireBizKeyAndTraceId(p, "applicationId"));
        assertEquals(true, ex.getMessage().contains("applicationId"));
    }

    @Test
    void 缺traceId拒绝() {
        String p = "{\"applicationId\":\"AP-001\"}";
        assertThrows(IllegalArgumentException.class,
                () -> JsonPayloadReader.requireBizKeyAndTraceId(p, "applicationId"));
    }

    @Test
    void 未声明任何主键是编程错误() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonPayloadReader.requireBizKeysAndTraceId(RF_PAYLOAD));
    }

    @Test
    void 非法JSON与空串显式抛错() {
        assertThrows(IllegalArgumentException.class, () -> JsonPayloadReader.readString("", "k"));
        assertThrows(IllegalArgumentException.class, () -> JsonPayloadReader.readString("not-json", "k"));
        assertThrows(IllegalArgumentException.class, () -> JsonPayloadReader.readString("[1,2]", "k"));
    }

    @Test
    void readLong接受数字与纯数字字符串_拒绝其他形状() {
        assertEquals(123456L, JsonPayloadReader.readLong(RF_PAYLOAD, "amount"));
        assertEquals(7L, JsonPayloadReader.readLong("{\"amount\":\"7\",\"traceId\":\"t\"}", "amount"));
        assertNull(JsonPayloadReader.readLong("{\"amount\":\"12.5\",\"traceId\":\"t\"}", "amount"));
        assertNull(JsonPayloadReader.readLong(RF_PAYLOAD, "missing"));
    }

    @Test
    void readString数字按文本返回_缺字段返回null() {
        assertEquals("123456", JsonPayloadReader.readString(RF_PAYLOAD, "amount"));
        assertNull(JsonPayloadReader.readString(RF_PAYLOAD, "missing"));
    }
}
