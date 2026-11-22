package com.company.platform.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 字段级 AES-GCM 加解密（机制层，非 Spring Bean——使用方自行装配）。
 *
 * <p><b>为什么用 GCM 而不是 CBC</b>：GCM 带认证标签，能发现密文被篡改；
 * CBC 不带认证，密文被改动后解密会静默产出垃圾明文——对「改一位就打到别人账户」的场景，
 * 静默失败不可接受。</p>
 *
 * <p><b>密文格式</b>：{@code base64(iv || ciphertext || tag)}。
 * IV 每次随机（12 字节），同一明文两次加密结果不同——防止"密文相同即明文相同"的关联分析。</p>
 *
 * <p><b>检索</b>：密文不可检索；需要等值检索的字段另存 HMAC 指纹列（见 CONVENTIONS.md）。
 * <b>密钥来源</b>：配置注入（环境变量），不入库不入代码；轮换用 key_version 字段标记，
 * 本类不管理轮换。</p>
 */
public final class AesGcmFieldCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    /**
     * @param key 密钥材料；不是 32 字节时用 SHA-256 派生到 32 字节（保证任何配置长度可用）。
     */
    public AesGcmFieldCipher(String key) {
        byte[] material = key.getBytes(StandardCharsets.UTF_8);
        if (material.length != 32) {
            try {
                material = MessageDigest.getInstance("SHA-256").digest(material);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 不可用，无法派生字段加密密钥", e);
            }
        }
        this.keySpec = new SecretKeySpec(material, "AES");
    }

    /** 加密明文；null 透传 null。失败（环境缺 JCE 等）抛 {@link IllegalStateException}。 */
    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("字段加密失败", e);
        }
    }

    /**
     * 解密密文；null 透传 null。
     * 密文被篡改时 GCM 认证失败抛 {@link IllegalStateException}——绝不静默产出垃圾明文。
     */
    public String decrypt(String cipherBase64) {
        if (cipherBase64 == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherBase64);
            if (combined.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文长度不合法");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("字段解密失败（密文损坏或密钥不符，或被篡改）", e);
        }
    }
}
