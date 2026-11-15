package com.openaidatalake.console.s3;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * 与网关同一套 SigV4。控制台只用它去列出远端桶和前缀。
 */
public final class S3Signer {

    public static final String EMPTY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private S3Signer() {
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
            headers.append(name).append(':').append(value.trim().replaceAll("\\s+", " ")).append('\n');
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

    public static String encode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder encoded = new StringBuilder();
        for (byte current : value.getBytes(StandardCharsets.UTF_8)) {
            int b = current & 0xff;
            if (isUnreserved(b)) {
                encoded.append((char) b);
            } else {
                encoded.append('%').append(HEX[b >>> 4]).append(HEX[b & 0x0f]);
            }
        }
        return encoded.toString();
    }

    private static String sha256Hex(String value) {
        try {
            return toHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
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

    private static String toHex(byte[] bytes) {
        char[] digits = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            out[i * 2] = digits[value >>> 4];
            out[i * 2 + 1] = digits[value & 0x0f];
        }
        return new String(out);
    }

    private static boolean isUnreserved(int b) {
        return (b >= 'A' && b <= 'Z')
                || (b >= 'a' && b <= 'z')
                || (b >= '0' && b <= '9')
                || b == '-' || b == '_' || b == '.' || b == '~';
    }
}
