package com.company.platform.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoTest {

    @Test
    void aesGcm加解密往返() {
        AesGcmFieldCipher c = new AesGcmFieldCipher("dev-key-change-me-32bytes-exactly!");
        String cipher = c.encrypt("1234567890");
        assertEquals("1234567890", c.decrypt(cipher));
    }

    @Test
    void 短密钥自动派生到32字节() {
        AesGcmFieldCipher c = new AesGcmFieldCipher("short");
        String cipher = c.encrypt("hello");
        assertEquals("hello", c.decrypt(cipher));
    }

    @Test
    void 同一明文两次加密结果不同_防关联分析() {
        AesGcmFieldCipher c = new AesGcmFieldCipher("dev-key-change-me-32bytes-exactly!");
        assertNotEquals(c.encrypt("A1B2C3"), c.encrypt("A1B2C3"));
    }

    @Test
    void 篡改密文解密显式失败_不静默产出垃圾() {
        AesGcmFieldCipher c = new AesGcmFieldCipher("dev-key-change-me-32bytes-exactly!");
        String cipher = c.encrypt("secret-data");
        byte[] raw = java.util.Base64.getDecoder().decode(cipher);
        raw[raw.length - 1] ^= 0x01;
        String tampered = java.util.Base64.getEncoder().encodeToString(raw);
        assertThrows(IllegalStateException.class, () -> c.decrypt(tampered));
    }

    @Test
    void null透传() {
        AesGcmFieldCipher c = new AesGcmFieldCipher("k");
        assertNull(c.encrypt(null));
        assertNull(c.decrypt(null));
    }

    @Test
    void 脱敏保边脱中() {
        assertEquals("山**郎", PiiMasker.maskMiddle("山田太郎", 1));
        assertEquals("**", PiiMasker.maskMiddle("ab", 1));
    }

    @Test
    void 脱敏保尾脱头() {
        assertEquals("****5678", PiiMasker.maskTail("12345678", 4));
        assertEquals("***", PiiMasker.maskTail("123", 4));
    }

    @Test
    void 掩码结果不含原文中间段() {
        String masked = PiiMasker.maskTail("999912345678", 4);
        assertEquals("********5678", masked);
    }
}
