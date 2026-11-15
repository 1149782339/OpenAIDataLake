package com.openaidatalake.gateway.s3;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class AwsUri {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private AwsUri() {
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

    public static String encodePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String[] parts = path.split("/", -1);
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(encode(parts[i]));
        }
        return encoded.toString();
    }

    public static String encodeAbsolutePath(String decodedPath) {
        String path = decodedPath == null || decodedPath.isEmpty() ? "/" : decodedPath;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "/" + encodePath(path.substring(1));
    }

    public static String decodePercent(String value) {
        if (value == null || value.isEmpty() || value.indexOf('%') < 0) {
            return value == null ? "" : value;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                if (high >= 0 && low >= 0) {
                    out.write((high << 4) + low);
                    i += 2;
                    continue;
                }
            }
            out.writeBytes(String.valueOf(current).getBytes(StandardCharsets.UTF_8));
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    public static List<String> splitPreserve(String raw, char separator) {
        List<String> parts = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return parts;
        }
        int start = 0;
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == separator) {
                parts.add(raw.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(raw.substring(start));
        return parts;
    }

    private static boolean isUnreserved(int b) {
        return (b >= 'A' && b <= 'Z')
                || (b >= 'a' && b <= 'z')
                || (b >= '0' && b <= '9')
                || b == '-' || b == '_' || b == '.' || b == '~';
    }
}
