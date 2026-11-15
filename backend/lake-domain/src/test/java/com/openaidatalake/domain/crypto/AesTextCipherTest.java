package com.openaidatalake.domain.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesTextCipherTest {

    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void roundTrip() {
        AesTextCipher cipher = AesTextCipher.fromBase64Key(KEY);
        String encrypted = cipher.encrypt("secret-key");
        assertNotEquals("secret-key", encrypted);
        assertEquals("secret-key", cipher.decrypt(encrypted));
    }

    @Test
    void rejectsShortKey() {
        assertThrows(IllegalArgumentException.class, () -> AesTextCipher.fromBase64Key("YQ=="));
    }
}
