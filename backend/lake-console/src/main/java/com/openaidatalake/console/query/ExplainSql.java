package com.openaidatalake.console.query;

import com.openaidatalake.console.web.ApiException;

/**
 * EXPLAIN 本身没有查询计划可序列化。剥掉前缀后，按里面的语句做权限检查。
 */
final class ExplainSql {

    record Prefix(boolean analyze, String statement) {
    }

    private ExplainSql() {
    }

    static Prefix parse(String sql) {
        Scanner scanner = new Scanner(sql);
        scanner.next();
        if (!scanner.is("explain")) {
            return null;
        }
        scanner.next();
        boolean analyze = false;
        if (scanner.is("(")) {
            analyze = scanner.optionListHasAnalyze();
        } else if (scanner.is("analyze")) {
            analyze = true;
            scanner.next();
        }
        if (scanner.is("verbose")) {
            scanner.next();
        }
        if (scanner.end()) {
            throw ApiException.bad("无法解析 SQL");
        }
        return new Prefix(analyze, sql.substring(scanner.start()).trim());
    }

    private static final class Scanner {
        private final String sql;
        private int index;
        private int start;
        private int depth;
        private String text = "";
        private boolean end;

        private Scanner(String sql) {
            this.sql = sql;
        }

        private boolean end() {
            return end;
        }

        private int start() {
            return start;
        }

        private boolean is(String expected) {
            return !end && text.equalsIgnoreCase(expected);
        }

        private boolean optionListHasAnalyze() {
            int target = depth - 1;
            next();
            boolean analyze = false;
            while (!end && depth > target) {
                if (is("analyze")) {
                    analyze = true;
                }
                next();
            }
            if (!is(")")) {
                throw ApiException.bad("无法解析 SQL");
            }
            next();
            return analyze;
        }

        private void next() {
            while (true) {
                skipTrivia();
                if (index >= sql.length()) {
                    start = index;
                    text = "";
                    end = true;
                    return;
                }
                start = index;
                char current = sql.charAt(index);
                if (current == ';') {
                    throw ApiException.bad("一次只能执行一条语句");
                }
                if (current == '\'') {
                    skipString();
                    continue;
                }
                if (current == '(') {
                    depth++;
                    index++;
                    text = "(";
                    end = false;
                    return;
                }
                if (current == ')') {
                    depth = Math.max(0, depth - 1);
                    index++;
                    text = ")";
                    end = false;
                    return;
                }
                if (Character.isLetter(current) || current == '_') {
                    int begin = index++;
                    while (index < sql.length()) {
                        char next = sql.charAt(index);
                        if (!Character.isLetterOrDigit(next) && next != '_') {
                            break;
                        }
                        index++;
                    }
                    text = sql.substring(begin, index);
                    end = false;
                    return;
                }
                index++;
                text = String.valueOf(current);
                end = false;
                return;
            }
        }

        private void skipTrivia() {
            while (index < sql.length()) {
                char current = sql.charAt(index);
                if (Character.isWhitespace(current)) {
                    index++;
                    continue;
                }
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
                return;
            }
        }

        private void skipString() {
            index++;
            while (index < sql.length()) {
                if (sql.charAt(index) == '\'') {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == '\'') {
                        index += 2;
                        continue;
                    }
                    index++;
                    return;
                }
                index++;
            }
            throw ApiException.bad("SQL 字符串没有结束");
        }
    }
}
