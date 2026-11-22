# company-platform-contracts · 公司级共享内核

**只放机制，不放语义。** groupId/artifactId：`com.company:company-platform-contracts`，语义化版本，使用方各自锁定。

## 内容边界

| 包 | 内容 | 来源 |
|---|---|---|
| `response` | `ApiResponse<T>`、`PageResponse<T>` | floorplan common 同构复制 |
| `exception` | `ErrorCode` 接口、`StandardErrorCode` 兜底码、`BusinessException`、`GlobalExceptionHandler` 骨架 | 复制 + 错误码接口化 |
| `security` | `SubjectType` 主体类型、`TokenVerifier` 端口、`VerifiedToken`、`PlatformJwtAuthInterceptor`（三道关）、`@RequirePermission` / `@RequireAnyPermission` / `@RequireSubject`、`AuditActor` | 复制 + 切断点 ③ 参数化 |
| `event` | `DomainEvent`、`EventConsumer`、`OutboxPublisher` 端口（无事件常量）、`JsonPayloadReader`（**切断点 ①：`requireBizKeyAndTraceId(bizKeyName)`**） | 复制 + 切断点 ① 参数化 |
| `idempotency` | `IdempotencyRecord`、`IdempotencyStore` 端口、`IdempotencyGuard`（重放/冲突语义） | 新建（语义与 floorplan CONFLICT_001 对齐） |
| `calendar` | `BusinessCalendar` 端口 | floorplan 同构复制 |
| `crypto` | `AesGcmFieldCipher`、`PiiMasker` | 复制 + 去 Spring 化 + mask 泛化 |
| `storage` | `AttachmentStorage`、`DirectUploadEndpoint` 端口 | floorplan 同构复制 |
| `audit` | `AuditEvent` 结构 + append-only 约定 | 新建（各项目审计实现对齐此结构） |
| `CONVENTIONS.md` | 金额/时间/枚举/删除/traceId/payload 规范 | 新建 |

**明确不放**：任何业务错误码内容、权限点枚举、角色、实体、VIN/客户号语义、任何业务规则。

## 治理规则

1. 本 artifact **不 import 任何业务包**（`com.company.floorplan.*` / `com.company.retail.*`）——CI 应校验。
2. floorplan **不强制迁移**：floorplan 继续用 `com.company.floorplan.*` 现网类；未来迁移另行评审。
3. 新项目（RF 等）直接依赖本 artifact。
4. 变更流程：提案 → 影响面清单（两项目）→ 双方评审 → 新版本发布 → 各项目自行升级并跑全量验收。
5. 不兼容变更必须升大版本。

## 构建

```bash
mvn -B clean install   # 单测全绿后安装到本地仓库
```

依赖版本与 Spring Boot 3.2.5 对齐（Jackson 2.15.4 / Spring 6.1.6）；spring-web/webmvc/servlet-api/slf4j 为 `provided`——由使用方的运行时提供。
