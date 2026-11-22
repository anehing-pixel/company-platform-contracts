package com.company.platform.event;

import java.time.Instant;

/**
 * 领域事件的不可变载体（机制层）。
 *
 * <p>它代表 outbox 表里<b>已经发生</b>的一件事后的一条不可变记录：由生产方在业务事务内写入
 * （见 {@link OutboxPublisher}），由消费方按游标拉取。只有字段、没有行为。</p>
 *
 * <p><b>为什么用 record</b>：事件一旦提交就不可修改；可变 bean 会让消费者之间互相改 payload，
 * 造成"同一个事件在不同服务里含义不同"的隐性 bug。</p>
 *
 * <p><b>payload 为什么是 String</b>：库里存的就是 JSON 文本，各消费方关心的字段不同。
 * 由消费方用 {@link JsonPayloadReader} 按需要的形状反序列化——共享层不为任何消费方
 * 定义"全量 DTO"，那会变成第二个契约源。</p>
 *
 * <p><b>与 floorplan 版的差异（切断点 ①）</b>：aggregateKey 与 payload 必填键不再绑定 {@code vin}，
 * 业务主键由各项目通过 {@link JsonPayloadReader#requireBizKeyAndTraceId} 自行声明。</p>
 *
 * @param eventId      事件 ID，全局单调递增，消费游标与幂等键的主键
 * @param eventType    事件名，各项目自行定义常量集合，不许自由命名
 * @param aggregateKey 聚合键（该项目的业务主键），发布去重维度
 * @param payload      JSON 文本，必含声明的业务主键与 {@code traceId}
 * @param occurredAt   事件写入时间（TIMESTAMPTZ），业务判断以它为准
 */
public record DomainEvent(long eventId, String eventType, String aggregateKey,
                          String payload, Instant occurredAt) {

    /** 从 payload 里取一个字符串字段；取不到返回 null，由调用方决定跳过还是失败。 */
    public String textField(String key) {
        return JsonPayloadReader.readString(payload, key);
    }

    /** 从 payload 里取一个整数字段（金额、ID）；非整数返回 null。 */
    public Long longField(String key) {
        return JsonPayloadReader.readLong(payload, key);
    }
}
