package com.openaidatalake.gateway.s3;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SigV4 {

    public static final String EMPTY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private SigV4() {
    }

    public static String canonicalRequest(String method,
                                          String canonicalUri,
                                          String canonicalQuery,
                                          List<String> signedHeaders,
                                          Map<String, String> headerValues,
                                          String payloadHash) {
        StringBuilder headers = new StringBuilder();
        for (String name : signedHeaders) {
            String value = headerValues.get(name);
            if (value == null) {
                throw new IllegalArgumentException("缺少签名头 " + name);
            }
            headers.append(name).append(':').append(normalizeHeader(value)).append('\n');
        }
        return method + "\n"
                + canonicalUri + "\n"
                + canonicalQuery + "\n"
                + headers
                + "\n"
                + String.join(";", signedHeaders) + "\n"
                + payloadHash;
    }

    public static String signature(String secret,
                                   String amzDate,
                                   String scopeDate,
                                   String region,
                                   String canonicalRequest) {
        String scope = scopeDate + "/" + region + "/s3/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + scope + "\n" + sha256Hex(canonicalRequest);
        byte[] key = signingKey(secret, scopeDate, region);
        return toHex(hmac(key, stringToSign.getBytes(StandardCharsets.UTF_8)));
    }

    public static String authorization(String accessKey,
                                       String scopeDate,
                                       String region,
                                       String signedHeaders,
                                       String signature) {
        return "AWS4-HMAC-SHA256 Credential=" + accessKey + "/" + scopeDate + "/" + region + "/s3/aws4_request"
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;
    }

    public static String sha256Hex(String value) {
        return sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] value) {
        try {
            return toHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String normalizeHeader(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    public static boolean sameSignature(String expectedHex, String actualHex) {
        if (expectedHex == null || actualHex == null || expectedHex.length() != actualHex.length()) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                actualHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] signingKey(String secret, String scopeDate, String region) {
        byte[] date = hmac(("AWS4" + secret).getBytes(StandardCharsets.UTF_8), scopeDate.getBytes(StandardCharsets.UTF_8));
        byte[] regionKey = hmac(date, region.getBytes(StandardCharsets.UTF_8));
        byte[] service = hmac(regionKey, "s3".getBytes(StandardCharsets.UTF_8));
        return hmac(service, "aws4_request".getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String toHex(byte[] bytes) {
        char[] digits = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            out[i * 2] = digits[value >>> 4];
            out[i * 2 + 1] = digits[value & 0x0f];
        }
        return new String(out);
    }
}
