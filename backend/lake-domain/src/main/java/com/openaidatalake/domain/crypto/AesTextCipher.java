package com.openaidatalake.domain.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM。密文格式为 {@code v1:} 加 Base64(12 字节 IV + 密文和认证标签)。
 */
public final class AesTextCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final String PREFIX = "v1:";

    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    private AesTextCipher(byte[] key) {
        this.key = key.clone();
    }

    public static AesTextCipher fromBase64Key(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("缺少 LAKE_MASTER_KEY");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("LAKE_MASTER_KEY 不是合法的 Base64", ex);
        }
        if (key.length != 32) {
            throw new IllegalArgumentException("LAKE_MASTER_KEY 解码后必须是 32 字节");
        }
        return new AesTextCipher(key);
    }

    public String encrypt(String plain) {
        if (plain == null) {
            throw new IllegalArgumentException("明文不能为空");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] packed = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(packed);
        } catch (Exception ex) {
            throw new IllegalStateException("加密失败", ex);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            throw new IllegalArgumentException("密文格式不正确");
        }
        try {
            byte[] packed = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (packed.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文太短");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(packed, 0, iv, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(packed, IV_LENGTH, packed.length - IV_LENGTH);
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("解密失败", ex);
        }
    }
}
