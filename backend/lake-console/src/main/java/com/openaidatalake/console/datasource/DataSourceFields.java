package com.openaidatalake.console.datasource;

import com.openaidatalake.console.web.ApiException;
import com.openaidatalake.domain.datasource.DataSourceEngine;

import java.util.regex.Pattern;

public final class DataSourceFields {

    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,62}$");
    private static final Pattern HOST = Pattern.compile("^(?:[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?|(?:\\d{1,3}\\.){3}\\d{1,3})$");
    private static final Pattern PLAIN = Pattern.compile("^[^\\s'\"\\\\/;]{1,128}$");

    private DataSourceFields() {
    }

    public static String name(String raw) {
        String value = text(raw, "请填写数据源名称");
        if (!NAME.matcher(value).matches()) {
            throw ApiException.bad("数据源名称只能使用英文字母、数字和下划线，并且以字母开头");
        }
        return value;
    }

    public static DataSourceEngine engine(String raw) {
        try {
            return DataSourceEngine.fromLabel(text(raw, "请填写数据源类型"));
        } catch (IllegalArgumentException ex) {
            throw ApiException.bad(ex.getMessage());
        }
    }

    public static String host(String raw) {
        String value = text(raw, "请填写 Host");
        if (!HOST.matcher(value).matches()) {
            throw ApiException.bad("Host 只能是域名或 IP");
        }
        return value;
    }

    public static int port(Integer raw) {
        if (raw == null || raw < 1 || raw > 65535) {
            throw ApiException.bad("端口必须是 1 到 65535 的整数");
        }
        return raw;
    }

    public static String user(String raw) {
        return plain(raw, "用户");
    }

    public static String database(String raw) {
        return plain(raw, "数据库名称");
    }

    public static String password(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw ApiException.bad("请填写密码");
        }
        if (raw.length() > 256 || raw.chars().anyMatch(ch -> ch < 0x20 || ch == 0x7f)) {
            throw ApiException.bad("密码包含不支持的字符");
        }
        return raw;
    }

    private static String plain(String raw, String label) {
        String value = text(raw, "请填写" + label);
        if (!PLAIN.matcher(value).matches()) {
            throw ApiException.bad(label + "不能包含空格、引号或斜线");
        }
        return value;
    }

    private static String text(String raw, String message) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.bad(message);
        }
        return raw.trim();
    }
}
