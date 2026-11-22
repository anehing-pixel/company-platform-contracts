package com.company.platform.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * payload JSON 的只读取数器（机制层，全公司唯一口径）。
 *
 * <p>outbox 表的 payload 是 TEXT 存的 JSON，发布端要校验、消费端要读。若两侧各写一套解析，
 * 就会出现"发布端认为合法、消费端读不出"的分裂口径。字段存在性与类型判定收敛到这一个类。</p>
 *
 * <p><b>不做的事</b>：不缓存、不校验业务口径、不抛业务异常码。读不到返回 null，
 * 由调用方决定"失败还是跳过"。</p>
 */
public final class JsonPayloadReader {

    /** ObjectMapper 线程安全，建一次即可。 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonPayloadReader() {
    }

    /**
     * 解析 payload 为 JSON 对象。非法 JSON / 空串 / 非对象一律抛 {@link IllegalArgumentException}：
     * 这是发布方的编程错误，不该被当成"业务上没有值"静默处理。
     */
    private static JsonNode tree(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("事件 payload 为空，无法解析");
        }
        JsonNode node;
        try {
            node = MAPPER.readTree(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("事件 payload 不是合法 JSON：" + payload, e);
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("事件 payload 不是 JSON 对象：" + payload);
        }
        return node;
    }

    /** 读字符串字段；字段不存在或为 JSON null 时返回 null（数字按文本返回）。 */
    public static String readString(String payload, String key) {
        JsonNode v = tree(payload).get(key);
        return v == null || v.isNull() ? null : v.asText();
    }

    /** 读整数字段（金额、ID）；字段不存在或形状不是整数返回 null，不做四舍五入猜测。 */
    public static Long readLong(String payload, String key) {
        JsonNode v = tree(payload).get(key);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.canConvertToLong()) {
            return v.asLong();
        }
        // 纯数字字符串（"12345"）在跨语言写入时常见，按整数接受；其余形状返回 null 交给调用方判失败。
        String text = v.asText();
        if (text != null && text.matches("-?\\d+")) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 校验 payload 必含<b>声明的业务主键</b>与 {@code traceId} 且都非空白（切断点 ① 参数化落点）。
     *
     * <p>不再绑定 {@code vin}：业务主键由调用方声明（floorplan 传 {@code "vin"}，
     * RF 传 {@code "applicationId"} / {@code "loanId"} 等）。缺主键消费端定位不到聚合，
     * 缺 traceId 跨服务排障断链。发布端在写库<b>之前</b>调用，让错误回到生产方栈上。</p>
     *
     * @param payload   事件 payload JSON 文本
     * @param bizKeyName 该项目该类事件声明的业务主键字段名
     */
    public static void requireBizKeyAndTraceId(String payload, String bizKeyName) {
        requireBizKeysAndTraceId(payload, new String[]{bizKeyName});
    }

    /**
     * 多业务主键变体：payload 必含全部声明的键与 {@code traceId}。
     *
     * @param payload   事件 payload JSON 文本
     * @param bizKeys   声明的业务主键字段名集合（至少一个）
     */
    public static void requireBizKeysAndTraceId(String payload, String... bizKeys) {
        if (bizKeys == null || bizKeys.length == 0) {
            throw new IllegalArgumentException("必须至少声明一个业务主键字段名");
        }
        JsonNode node = tree(payload);
        for (String key : bizKeys) {
            if (!isPresent(node.get(key))) {
                throw new IllegalArgumentException("事件 payload 缺少必填业务主键 " + key + "：" + payload);
            }
        }
        if (!isPresent(node.get("traceId"))) {
            throw new IllegalArgumentException("事件 payload 缺少必填字段 traceId（跨服务排障要求）：" + payload);
        }
    }

    /** 字段存在、非 JSON null、且文本不为空白，才算"有值"。 */
    private static boolean isPresent(JsonNode v) {
        return v != null && !v.isNull() && !v.asText().isBlank();
    }
}
