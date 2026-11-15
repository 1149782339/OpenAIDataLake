package com.openaidatalake.console.query;

import com.openaidatalake.console.web.ApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * json_serialize_plan 序列化不了 Postgres / MySQL 扫描。
 * 这种写语句仍按 INSERT / UPDATE / DELETE 的形状取出目标表和其中的查询。
 */
final class WriteShape {

    record Parsed(String targetCatalog, String query, List<String> reads) {
    }

    private WriteShape() {
    }

    static Parsed parse(String sql) {
        Scanner scanner = new Scanner(sql);
        scanner.next();
        List<String> reads = new ArrayList<>();
        if (scanner.is("with")) {
            parseCtes(scanner, reads);
        }
        if (scanner.is("insert")) {
            return parseInsert(scanner, reads);
        }
        if (scanner.is("update")) {
            return parseUpdate(scanner, reads);
        }
        if (scanner.is("delete")) {
            return parseDelete(scanner, reads);
        }
        throw unrecognized();
    }

    private static Parsed parseInsert(Scanner scanner, List<String> reads) {
        scanner.next();
        if (scanner.is("or")) {
            scanner.next();
            if (!scanner.is("replace") && !scanner.is("ignore")) {
                throw unrecognized();
            }
            scanner.next();
        }
        if (!scanner.is("into")) {
            throw unrecognized();
        }
        scanner.next();
        String catalog = catalogOf(scanner);
        if (scanner.is("(") && !scanner.followedByQuery()) {
            scanner.skipGroup();
        }
        if (scanner.is("by")) {
            scanner.next();
            if (!scanner.is("name") && !scanner.is("position")) {
                throw unrecognized();
            }
            scanner.next();
        }
        if (scanner.is("overriding")) {
            scanner.next();
            if (!scanner.is("system") && !scanner.is("user")) {
                throw unrecognized();
            }
            scanner.next();
            if (scanner.is("value")) {
                scanner.next();
            }
        }
        if (scanner.is("select") || scanner.is("with") || scanner.is("table") || scanner.is("(")) {
            boolean wrapped = scanner.is("(");
            String query = scanner.takeQuery();
            if (wrapped && query.startsWith("(") && query.endsWith(")")) {
                query = query.substring(1, query.length() - 1).trim();
            }
            scanTail(scanner, reads, null);
            return new Parsed(catalog, query, List.copyOf(reads));
        }
        scanTail(scanner, reads, null);
        return new Parsed(catalog, null, List.copyOf(reads));
    }

    private static Parsed parseUpdate(Scanner scanner, List<String> reads) {
        scanner.next();
        String catalog = catalogOf(scanner);
        if (!scanner.is("set")) {
            throw unrecognized();
        }
        scanner.next();
        scanTail(scanner, reads, "from");
        return new Parsed(catalog, null, List.copyOf(reads));
    }

    private static Parsed parseDelete(Scanner scanner, List<String> reads) {
        scanner.next();
        if (!scanner.is("from")) {
            throw unrecognized();
        }
        scanner.next();
        String catalog = catalogOf(scanner);
        scanTail(scanner, reads, "using");
        return new Parsed(catalog, null, List.copyOf(reads));
    }

    private static void parseCtes(Scanner scanner, List<String> reads) {
        scanner.next();
        if (scanner.is("recursive")) {
            scanner.next();
        }
        while (true) {
            if (!scanner.isWord()) {
                throw unrecognized();
            }
            scanner.next();
            if (scanner.is("(")) {
                scanner.skipGroup();
            }
            if (!scanner.is("as")) {
                throw unrecognized();
            }
            scanner.next();
            if (scanner.is("not")) {
                scanner.next();
            }
            if (scanner.is("materialized")) {
                scanner.next();
            }
            if (!scanner.is("(")) {
                throw unrecognized();
            }
            reads.add(scanner.innerGroup());
            if (!scanner.is(",")) {
                return;
            }
            scanner.next();
        }
    }

    private static void scanTail(Scanner scanner, List<String> reads, String fromKeyword) {
        while (!scanner.end()) {
            if (scanner.depth() == 0 && fromKeyword != null && scanner.is(fromKeyword)) {
                scanner.next();
                int from = scanner.start();
                while (!scanner.end() && !(scanner.depth() == 0 && endsFrom(scanner))) {
                    scanner.next();
                }
                int to = scanner.end() ? scanner.sql.length() : scanner.start();
                String clause = scanner.sql.substring(from, to).trim();
                if (!clause.isEmpty()) {
                    reads.add("SELECT * FROM " + clause);
                }
                continue;
            }
            if (scanner.is("(") && scanner.followedByQuery()) {
                reads.add(scanner.innerGroup());
                continue;
            }
            scanner.next();
        }
    }

    private static boolean endsFrom(Scanner scanner) {
        return scanner.is("where") || scanner.is("returning");
    }

    private static String catalogOf(Scanner scanner) {
        if (!scanner.isWord()) {
            throw unrecognized();
        }
        String first = scanner.text();
        scanner.next();
        if (!scanner.is(".")) {
            return "";
        }
        scanner.next();
        if (!scanner.isWord()) {
            throw unrecognized();
        }
        scanner.next();
        while (scanner.is(".")) {
            scanner.next();
            if (!scanner.isWord()) {
                throw unrecognized();
            }
            scanner.next();
        }
        return first;
    }

    private static ApiException unrecognized() {
        return ApiException.bad("无法识别这条写语句");
    }

    private static final class Scanner {
        private final String sql;
        private int index;
        private int start;
        private int depth;
        private String text = "";
        private boolean word;
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

        private int depth() {
            return depth;
        }

        private String text() {
            return text;
        }

        private boolean isWord() {
            return word && !end;
        }

        private boolean is(String expected) {
            return !end && text.equalsIgnoreCase(expected);
        }

        private boolean followedByQuery() {
            Mark mark = mark();
            next();
            boolean query = is("select") || is("with") || is("table") || is("values");
            mark.restore();
            return query;
        }

        private void skipGroup() {
            if (!"(".equals(text)) {
                throw unrecognized();
            }
            int target = depth - 1;
            next();
            while (!end && depth > target) {
                next();
            }
            if (!")".equals(text)) {
                throw unrecognized();
            }
            next();
        }

        private String innerGroup() {
            int open = start;
            if (!"(".equals(text)) {
                throw unrecognized();
            }
            int target = depth - 1;
            next();
            while (!end && depth > target) {
                next();
            }
            if (!")".equals(text)) {
                throw unrecognized();
            }
            int close = start;
            next();
            return sql.substring(open + 1, close).trim();
        }

        private String takeQuery() {
            int from = start;
            int base = "(".equals(text) ? depth - 1 : depth;
            while (!end) {
                if (depth == base && is("returning")) {
                    break;
                }
                if (depth == base && is("on") && followedBy("conflict")) {
                    break;
                }
                next();
            }
            int to = end ? sql.length() : start;
            return sql.substring(from, to).trim();
        }

        private boolean followedBy(String expected) {
            Mark mark = mark();
            next();
            boolean match = is(expected);
            mark.restore();
            return match;
        }

        private void next() {
            while (true) {
                skipTrivia();
                if (index >= sql.length()) {
                    start = index;
                    text = "";
                    word = false;
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
                if (current == '$') {
                    if (skipDollar()) {
                        continue;
                    }
                    index++;
                    text = "$";
                    word = false;
                    end = false;
                    return;
                }
                if (current == '"') {
                    text = quoted();
                    word = true;
                    end = false;
                    return;
                }
                if (Character.isLetter(current) || current == '_') {
                    int begin = index;
                    index++;
                    while (index < sql.length()) {
                        char next = sql.charAt(index);
                        if (!Character.isLetterOrDigit(next) && next != '_') {
                            break;
                        }
                        index++;
                    }
                    text = sql.substring(begin, index);
                    word = true;
                    end = false;
                    return;
                }
                if (Character.isDigit(current)) {
                    index++;
                    while (index < sql.length()) {
                        char next = sql.charAt(index);
                        if (!Character.isLetterOrDigit(next) && next != '.' && next != '_') {
                            break;
                        }
                        index++;
                    }
                    text = "";
                    word = false;
                    end = false;
                    return;
                }
                if (current == '(') {
                    depth++;
                } else if (current == ')') {
                    depth = Math.max(0, depth - 1);
                }
                index++;
                if (index < sql.length()) {
                    char next = sql.charAt(index);
                    if ((current == '<' && (next == '>' || next == '='))
                            || (current == '>' && next == '=')
                            || (current == '|' && next == '|')
                            || (current == ':' && next == ':')) {
                        index++;
                    }
                }
                text = sql.substring(start, index);
                word = false;
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

        private boolean skipDollar() {
            int tag = index + 1;
            int endTag = tag;
            while (endTag < sql.length()) {
                char current = sql.charAt(endTag);
                if (!Character.isLetterOrDigit(current) && current != '_') {
                    break;
                }
                endTag++;
            }
            if (endTag >= sql.length() || sql.charAt(endTag) != '$') {
                return false;
            }
            String closer = sql.substring(index, endTag + 1);
            int body = sql.indexOf(closer, endTag + 1);
            if (body < 0) {
                throw ApiException.bad("SQL 字符串没有结束");
            }
            index = body + closer.length();
            return true;
        }

        private String quoted() {
            index++;
            StringBuilder builder = new StringBuilder();
            while (index < sql.length()) {
                char current = sql.charAt(index);
                if (current == '"') {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == '"') {
                        builder.append('"');
                        index += 2;
                        continue;
                    }
                    index++;
                    return builder.toString();
                }
                builder.append(current);
                index++;
            }
            throw unrecognized();
        }

        private Mark mark() {
            return new Mark(index, start, depth, text, word, end);
        }

        private final class Mark {
            private final int index;
            private final int start;
            private final int depth;
            private final String text;
            private final boolean word;
            private final boolean end;

            private Mark(int index, int start, int depth, String text, boolean word, boolean end) {
                this.index = index;
                this.start = start;
                this.depth = depth;
                this.text = text;
                this.word = word;
                this.end = end;
            }

            private void restore() {
                Scanner.this.index = index;
                Scanner.this.start = start;
                Scanner.this.depth = depth;
                Scanner.this.text = text;
                Scanner.this.word = word;
                Scanner.this.end = end;
            }
        }
    }
}
