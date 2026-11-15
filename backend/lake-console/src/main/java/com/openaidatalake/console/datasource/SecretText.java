package com.openaidatalake.console.datasource;

public final class SecretText {

    private SecretText() {
    }

    public static String redact(String message, String password) {
        if (message == null || message.isBlank()) {
            return "未知错误";
        }
        String cleaned = message;
        if (password != null && !password.isEmpty()) {
            cleaned = cleaned.replace(password, "******");
        }
        cleaned = cleaned.replaceAll("(?i)password\\s+'[^']*'", "PASSWORD '******'");
        cleaned = cleaned.replaceAll("(?i)password\\s*=\\s*'[^']*'", "password='******'");
        cleaned = cleaned.replaceAll("(?i)password\\s*=\\s*\\S+", "password=******");
        return cleaned;
    }

    public static String limit(String message) {
        if (message.length() <= 300) {
            return message;
        }
        return message.substring(0, 300);
    }
}
