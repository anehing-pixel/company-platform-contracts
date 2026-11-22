package com.company.platform.storage;

import java.time.Duration;

/**
 * 附件存储接缝（机制层）。
 *
 * <p><b>核心约束：预签名直传，文件不过应用。</b>文件本体由存储端直接与客户端交互，
 * 应用只负责签发授权、回读元数据、签发访问授权。RF 当前用本地磁盘实现
 * （{@link DirectUploadEndpoint}），S3 等对象存储是同一端口的另一实现。</p>
 *
 * <p>与 floorplan 版同构。</p>
 */
public interface AttachmentStorage {

    /** 上传授权。 */
    record PresignedUpload(String uploadUrl, String objectKey, Duration expiresIn, String contentType, long contentLength) {
    }

    /** 对象元数据（回读校验用）。 */
    record ObjectMetadata(String objectKey, long contentLength, String contentType, String eTag, String sha256) {
    }

    /**
     * 签发上传授权。
     *
     * <p><b>签名参数必须锁定单对象 key + Content-Type + Content-Length</b>，
     * 否则"授权 A 传 B"就成立。</p>
     *
     * @param objectKey     对象键（UUID/哈希，不用原始文件名）
     * @param contentType   内容类型
     * @param contentLength 内容长度（字节）
     * @param expiresIn     有效期
     */
    PresignedUpload presignUpload(String objectKey, String contentType, long contentLength, Duration expiresIn);

    /** 回读对象元数据（上传完成登记时校验）；对象不存在返回 {@code null}。 */
    ObjectMetadata head(String objectKey);

    /** 签发访问授权（短时效预签名 GET URL）；对象不存在返回 {@code null}。 */
    String presignGet(String objectKey, Duration expiresIn);

    /** 存储实现标识（如 {@code local} / {@code s3}），用于后台展示与排查。 */
    String providerName();
}
