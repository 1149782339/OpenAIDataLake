package com.openaidatalake.console.query;

import com.openaidatalake.console.web.ApiException;

import java.util.Locale;

public final class SqlGuard {

    private static final String[] BLOCKED = {
            "postgres_query(",
            "postgres_execute(",
            "mysql_query(",
            "mysql_execute(",
            "clickhouse_query(",
            "clickhouse_execute(",
            "clickhouse_scan(",
            "read_csv(",
            "read_blob(",
            "read_text(",
            "read_json(",
            "parquet_scan(",
            "glob("
    };

    private SqlGuard() {
    }

    /** 去掉首尾空白和末尾一个分号。语句种类由 SqlGate 用 DuckDB 解析器判断。 */
    public static String body(String sql) {
        if (sql == null || sql.isBlank()) {
            throw ApiException.bad("请输入要执行的 SQL");
        }
        String body = sql.trim();
        if (body.endsWith(";")) {
            body = body.substring(0, body.length() - 1).trim();
        }
        if (body.isEmpty()) {
            throw ApiException.bad("请输入要执行的 SQL");
        }
        return body;
    }

    public static String wrapSelect(String sql) {
        return "SELECT * FROM (\n" + body(sql) + "\n) AS _lake_q LIMIT 1000";
    }

    public static String singleRead(String sql) {
        String body = body(sql);
        if (hasSemicolon(body)) {
            throw ApiException.bad("一次只能执行一条查询");
        }
        String keyword = firstKeyword(body);
        if (!"select".equals(keyword) && !"with".equals(keyword)) {
            throw ApiException.bad("只允许 SELECT 查询");
        }
        String lower = body.toLowerCase(Locale.ROOT);
        for (String blocked : BLOCKED) {
            if (lower.contains(blocked)) {
                throw ApiException.bad("查询里不能使用 " + blocked.substring(0, blocked.length() - 1));
            }
        }
        return wrapSelect(body);
    }

    private static String firstKeyword(String sql) {
        int index = skipTrivia(sql, 0);
        int start = index;
        while (index < sql.length() && Character.isLetter(sql.charAt(index))) {
            index++;
        }
        return sql.substring(start, index).toLowerCase(Locale.ROOT);
    }

    private static boolean hasSemicolon(String sql) {
        int index = 0;
        while (index < sql.length()) {
            char current = sql.charAt(index);
            if (current == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                int next = sql.indexOf('\n', index);
                index = next < 0 ? sql.length() : next + 1;
                continue;
            }
            if (current == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                int next = sql.indexOf("*/", index + 2);
                if (next < 0) {
                    throw ApiException.bad("SQL 注释没有结束");
                }
                index = next + 2;
                continue;
            }
            if (current == '\'') {
                index = skipString(sql, index);
                continue;
            }
            if (current == ';') {
                return true;
            }
            index++;
        }
        return false;
    }

    private static int skipTrivia(String sql, int index) {
        while (index < sql.length()) {
            char current = sql.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            if (current == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                int next = sql.indexOf('\n', index);
                if (next < 0) {
                    return sql.length();
                }
                index = next + 1;
                continue;
            }
            if (current == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                int next = sql.indexOf("*/", index + 2);
                if (next < 0) {
                    throw ApiException.bad("SQL 注释没有结束");
                }
                index = next + 2;
                continue;
            }
            break;
        }
        return index;
    }

    private static int skipString(String sql, int index) {
        index++;
        while (index < sql.length()) {
            if (sql.charAt(index) == '\'') {
                if (index + 1 < sql.length() && sql.charAt(index + 1) == '\'') {
                    index += 2;
                    continue;
                }
                return index + 1;
            }
            index++;
        }
        throw ApiException.bad("SQL 字符串没有结束");
    }
}
