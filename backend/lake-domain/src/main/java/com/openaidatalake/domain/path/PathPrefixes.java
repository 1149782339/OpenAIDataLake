package com.openaidatalake.domain.path;

import java.util.regex.Pattern;

/**
 * 逻辑前缀按目录存储：空字符串表示整个虚拟桶，其余以 {@code /} 结尾。
 * 匹配按目录边界，避免 {@code camera} 盖住 {@code camera-2}。
 */
public final class PathPrefixes {

    private static final Pattern BUCKET = Pattern.compile("^[a-z0-9][a-z0-9._-]{1,61}[a-z0-9]$");

    private PathPrefixes() {
    }

    public static void checkBucket(String bucket) {
        if (bucket == null || !BUCKET.matcher(bucket).matches() || bucket.contains("..")) {
            throw new IllegalArgumentException("名称只能使用小写字母、数字、点、下划线和中横线，长度 3 到 63");
        }
    }

    public static String normalizePrefix(String raw) {
        if (raw == null || raw.isBlank() || "/".equals(raw.trim())) {
            return "";
        }
        String key = normalizeKey(raw.trim());
        if (key.endsWith("/")) {
            key = key.substring(0, key.length() - 1);
        }
        if (key.isEmpty()) {
            return "";
        }
        for (String part : key.split("/", -1)) {
            if (part.isEmpty()) {
                throw new IllegalArgumentException("路径不能包含空目录");
            }
        }
        return key + "/";
    }

    public static String normalizeKey(String raw) {
        String key = raw == null ? "" : raw;
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        if (key.contains("\\") || key.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("路径不合法");
        }
        if ("/".equals(key)) {
            return "";
        }
        for (String part : key.split("/", -1)) {
            if (".".equals(part) || "..".equals(part)) {
                throw new IllegalArgumentException("路径不合法");
            }
        }
        return key;
    }

    public static boolean covers(String prefix, String path) {
        String dir = prefix == null ? "" : prefix;
        String key = path == null ? "" : path;
        if (dir.isEmpty()) {
            return true;
        }
        if (!dir.endsWith("/")) {
            dir = dir + "/";
        }
        String bare = dir.substring(0, dir.length() - 1);
        return key.equals(dir) || key.equals(bare) || key.startsWith(dir);
    }

    public static String remainder(String prefix, String path) {
        if (!covers(prefix, path)) {
            throw new IllegalArgumentException("前缀盖不住该路径");
        }
        if (prefix == null || prefix.isEmpty()) {
            return path == null ? "" : path;
        }
        String dir = prefix.endsWith("/") ? prefix : prefix + "/";
        String key = path == null ? "" : path;
        if (key.startsWith(dir)) {
            return key.substring(dir.length());
        }
        return "";
    }

    public static String join(String physicalPrefix, String logicalPrefix, String logicalKey) {
        String rest = remainder(logicalPrefix, logicalKey);
        String base = physicalPrefix == null ? "" : physicalPrefix;
        if (rest.isEmpty() && (logicalKey == null || !logicalKey.endsWith("/")) && base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base + rest;
    }

    public static String toLogical(String physicalPrefix, String logicalPrefix, String physicalKey) {
        String base = physicalPrefix == null ? "" : physicalPrefix;
        String key = physicalKey == null ? "" : physicalKey;
        String rest;
        if (base.isEmpty()) {
            rest = key;
        } else if (key.startsWith(base)) {
            rest = key.substring(base.length());
        } else if (base.endsWith("/") && key.equals(base.substring(0, base.length() - 1))) {
            rest = "";
        } else {
            return null;
        }
        String logical = logicalPrefix == null ? "" : logicalPrefix;
        if (rest.isEmpty() && !key.endsWith("/") && logical.endsWith("/")) {
            return logical.substring(0, logical.length() - 1);
        }
        return logical + rest;
    }

    public static boolean overlaps(String left, String right) {
        if (left.equals(right)) {
            return true;
        }
        String bareLeft = stripSlash(left);
        String bareRight = stripSlash(right);
        return covers(left, bareRight) || covers(right, bareLeft);
    }

    private static String stripSlash(String prefix) {
        if (prefix.endsWith("/") && prefix.length() > 1) {
            return prefix.substring(0, prefix.length() - 1);
        }
        if ("/".equals(prefix)) {
            return "";
        }
        return prefix;
    }
}
