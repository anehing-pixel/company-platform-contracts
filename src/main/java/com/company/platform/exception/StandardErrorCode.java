package com.company.platform.exception;

/**
 * 平台级通用兜底错误码（机制层）。
 *
 * <p>只含「与业务语义无关」的三个通用码：入参非法、幂等冲突、系统异常。
 * 业务错误码一律由各项目自己的 enum 实现 {@link ErrorCode} 提供，<b>禁止</b>往这里加。</p>
 */
public enum StandardErrorCode implements ErrorCode {

    VALID_001("参数校验失败", 400),
    UNAUTHORIZED("未认证或令牌失效", 401),
    FORBIDDEN("无该操作权限", 403),
    CONFLICT_001("幂等键冲突，请求内容与首次不一致", 409),
    SYS_001("系统异常", 500);

    private final String message;
    private final int httpStatus;

    StandardErrorCode(String message, int httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }
}
