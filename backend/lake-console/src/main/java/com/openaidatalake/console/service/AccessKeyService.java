package com.openaidatalake.console.service;

import com.openaidatalake.console.web.ApiException;
import com.openaidatalake.domain.crypto.AesTextCipher;
import com.openaidatalake.domain.user.UserAccessKey;
import com.openaidatalake.domain.user.UserAccessKeyRepository;
import com.openaidatalake.domain.user.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Service
public class AccessKeyService {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final char[] ACCESS_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final UserAccessKeyRepository keys;
    private final UserAccountRepository users;
    private final AesTextCipher cipher;
    private final SecureRandom random = new SecureRandom();

    public AccessKeyService(UserAccessKeyRepository keys, UserAccountRepository users, AesTextCipher cipher) {
        this.keys = keys;
        this.users = users;
        this.cipher = cipher;
    }

    @Transactional(readOnly = true)
    public List<AccessKeyView> list(Long userId) {
        List<UserAccessKey> found = userId == null
                ? keys.findAllByOrderByIdDesc()
                : keys.findByUserIdOrderByIdDesc(userId);
        return found.stream().map(AccessKeyView::from).toList();
    }

    @Transactional
    public CreatedAccessKey create(long userId) {
        if (!users.existsById(userId)) {
            throw ApiException.notFound("用户不存在");
        }
        String accessKey = uniqueAccessKey();
        String secret = random(ALPHABET, 40);
        UserAccessKey saved = keys.save(new UserAccessKey(userId, accessKey, cipher.encrypt(secret)));
        return new CreatedAccessKey(saved.getId(), userId, accessKey, secret, saved.getCreatedAt());
    }

    @Transactional
    public void delete(long id) {
        if (!keys.existsById(id)) {
            throw ApiException.notFound("访问钥匙不存在");
        }
        keys.deleteById(id);
    }

    private String uniqueAccessKey() {
        for (int i = 0; i < 5; i++) {
            String candidate = "AK" + random(ACCESS_ALPHABET, 18);
            if (keys.findByAccessKey(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("生成 Access Key 失败");
    }

    private String random(char[] alphabet, int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = alphabet[random.nextInt(alphabet.length)];
        }
        return new String(chars);
    }

    public record AccessKeyView(long id, long userId, String accessKey, boolean enabled, Instant createdAt) {
        static AccessKeyView from(UserAccessKey key) {
            return new AccessKeyView(key.getId(), key.getUserId(), key.getAccessKey(), key.isEnabled(), key.getCreatedAt());
        }
    }

    public record CreatedAccessKey(long id, long userId, String accessKey, String secretKey, Instant createdAt) {
    }
}
