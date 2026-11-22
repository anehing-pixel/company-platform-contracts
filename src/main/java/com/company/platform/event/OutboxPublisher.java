package com.company.platform.event;

/**
 * 本地消息表（Transactional Outbox）发布端口（机制层）。
 *
 * <p>用于"事务提交后的状态同步"这类允许秒级延迟的事后动作：写入与业务在<b>同一个本地事务</b>内
 * 完成，再由定时任务投递，消费端靠唯一键幂等。</p>
 *
 * <p><b>为什么不用 Seata</b>：AT 模式的全局锁 + 回滚在金融账务上成本和风险都太高。
 * 门禁类即时拦截走同步调用（fail-closed），事后状态同步走 outbox，两者分工。</p>
 *
 * <p><b>与 floorplan 版的差异</b>：事件名常量是<b>语义</b>（各项目各自定义，
 * 如 floorplan 的 LOAN_CREATED / RF 的 retail.* 事件），不放平台层；
 * 平台只保证「同事务写入 + 发布前校验」的机制。</p>
 */
public interface OutboxPublisher {

    /**
     * 在调用方事务内写一个 outbox 事件。
     *
     * @param eventType    事件名（各项目自己的常量）
     * @param aggregateKey 聚合键（该项目的业务主键），消费端幂等锚点
     * @param payload      JSON 文本，必含该项目声明的业务主键 + traceId
     *                     （发布实现应先过 {@link JsonPayloadReader#requireBizKeyAndTraceId}）
     */
    void publish(String eventType, String aggregateKey, String payload);
}
