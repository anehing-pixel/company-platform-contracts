package com.company.platform.exception;

/**
 * 错误码契约（机制层）：只定义形状，<b>不含任何业务错误码内容</b>。
 *
 * <p>各项目用 enum 实现本接口（如 floorplan 的 {@code ErrorCode} 枚举、RF 的 {@code RF_*} 体系）；
 * 平台层只提供通用兜底码 {@link StandardErrorCode}。</p>
 */
public interface ErrorCode {

    /** 机器可读错误码，如 {@code VALID_001}、{@code RF_VALID_001}。 */
    String getCode();

    /** 人类可读消息。 */
    String getMessage();

    /** 对应 HTTP 状态码。 */
    int getHttpStatus();
}
