# 工程约定（convention）

全公司各项目统一的持久化与集成约定。机制层代码假定这些约定成立。

## 金额
- **BIGINT，最小货币单位**（日元 ¥：整数円；无小数）。金额永不使用浮点列。
- 多币种预留 `currency` 列（ISO 4217），默认 JPY。

## 时间
- **TIMESTAMPTZ（UTC 存储）**，Java 侧 `Instant`。业务上的"营业日"另用 `DATE` 列 + 营业日历端口。
- 禁止用应用服务器本地时区写库。

## 枚举
- 落库用 VARCHAR 存枚举名，MyBatis/JdbcTemplate 用显式 RowMapper 转换，禁止 ordinal。

## 删除
- **不物理删除**业务数据。终态用显式状态（如 CANCELLED / CLOSED）表达；
  个人信息的删除请求走匿名化（保留键、清密文），审计链永不删。

## 事件 payload
- 必含：该项目声明的业务主键（`JsonPayloadReader.requireBizKeyAndTraceId` 声明）+ `traceId`。
- 禁止：个人信息明文（姓名/地址/电话/证件号/账号等）。ID 与哈希可以。
- 发布端在事务内写库前校验；消费端解析统一走 `JsonPayloadReader`。

## traceId
- 请求入口生成（网关）或透传（`X-Trace-Id`）；异常响应必须带 traceId。
- 日志每行带 traceId；跨服务排障靠它串链。

## 审计
- 审计表 append-only：只 INSERT，不 UPDATE/DELETE。
- 操作人取 `AuditActor.resolve`（令牌身份优先）；系统动作为 `"system"`。

## 幂等
- 写操作接口支持 `Idempotency-Key` 请求头或业务唯一键；
- 重放（指纹相同）返回首次结果，冲突（指纹不同）返回 409 CONFLICT 语义。
