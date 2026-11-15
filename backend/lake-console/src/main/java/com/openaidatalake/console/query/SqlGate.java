package com.openaidatalake.console.query;

import com.openaidatalake.console.web.ApiException;
import org.springframework.boot.json.JsonParserFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 用 DuckDB 自己的解析器判断语句种类。
 * 查询走 json_serialize_sql 的语法树；写入走 json_serialize_plan 的逻辑计划。
 * 认不出对象、不止一条语句，或者出现绕过表名的函数时拒绝。
 */
public final class SqlGate {

    private static final Set<String> BLOCKED_FUNCTIONS = Set.of(
            "postgres_query",
            "postgres_execute",
            "mysql_query",
            "mysql_execute",
            "clickhouse_query",
            "clickhouse_execute",
            "clickhouse_scan",
            "read_csv",
            "read_csv_auto",
            "read_blob",
            "read_text",
            "read_json",
            "read_json_auto",
            "read_ndjson",
            "read_ndjson_auto",
            "read_parquet",
            "parquet_scan",
            "glob",
            "iceberg_scan",
            "delta_scan"
    );

    private static final Set<String> WRITE_OPERATORS = Set.of(
            "LOGICAL_INSERT",
            "LOGICAL_UPDATE",
            "LOGICAL_DELETE"
    );

    private SqlGate() {
    }

    public enum Mode {
        READ,
        WRITE
    }

    public record Verdict(Mode mode, String sql, Set<String> readCatalogs, Set<String> writeCatalogs, boolean direct) {
        public Verdict(Mode mode, String sql, Set<String> readCatalogs, Set<String> writeCatalogs) {
            this(mode, sql, readCatalogs, writeCatalogs, false);
        }
    }

    public static Verdict inspect(Connection connection, String sql) {
        String body = SqlGuard.body(sql);
        ExplainSql.Prefix explain = ExplainSql.parse(body);
        if (explain != null) {
            return explainVerdict(connection, body, explain);
        }
        ensureJson(connection);
        Map<String, Object> tree = parse(scalar(connection,
                "SELECT json_serialize_sql(?, skip_null := true, skip_empty := true)", body));
        if (isError(tree)) {
            String message = text(tree.get("error_message"));
            if (message.contains("Only SELECT statements can be serialized")) {
                return write(connection, body);
            }
            throw ApiException.bad(limit("无法解析 SQL：" + message));
        }
        return read(tree, body);
    }

    private static Verdict explainVerdict(Connection connection, String sql, ExplainSql.Prefix explain) {
        Verdict inner = inspect(connection, explain.statement());
        if (explain.analyze() && inner.mode() == Mode.WRITE) {
            return new Verdict(Mode.WRITE, sql, inner.readCatalogs(), inner.writeCatalogs(), true);
        }
        Set<String> reads = new LinkedHashSet<>(inner.readCatalogs());
        reads.addAll(inner.writeCatalogs());
        return new Verdict(Mode.READ, sql, Set.copyOf(reads), Set.of(), true);
    }

    private static Verdict read(Map<String, Object> tree, String sql) {
        List<Object> statements = list(tree.get("statements"));
        if (statements.size() != 1) {
            throw ApiException.bad("一次只能执行一条语句");
        }
        Map<String, Object> statement = object(statements.get(0));
        Map<String, Object> node = object(statement.get("node"));
        rejectFunctions(node);
        Set<String> catalogs = new LinkedHashSet<>();
        walkQuery(node, Set.of(), catalogs);
        return new Verdict(Mode.READ, sql, Set.copyOf(catalogs), Set.of());
    }

    private static Verdict write(Connection connection, String sql) {
        Map<String, Object> tree = parse(scalar(connection,
                "SELECT json_serialize_plan(?, skip_null := true, skip_empty := true)", sql));
        if (isError(tree)) {
            if (unserializableScan(tree)) {
                return writeFromShape(connection, sql);
            }
            throw ApiException.bad(limit("无法解析 SQL：" + text(tree.get("error_message"))));
        }
        List<Object> plans = list(tree.get("plans"));
        if (plans.size() != 1) {
            throw ApiException.bad("一次只能执行一条语句");
        }
        Map<String, Object> plan = object(plans.get(0));
        String type = text(plan.get("type"));
        if (!WRITE_OPERATORS.contains(type)) {
            throw ApiException.bad("不支持这类语句");
        }
        rejectFunctions(plan);
        if (!hasTable(plan)) {
            throw ApiException.bad("写语句没有明确的目标表");
        }
        Set<String> writes = targetCatalogs(plan);
        Set<String> reads = new LinkedHashSet<>();
        collectScans(plan, reads);
        return new Verdict(Mode.WRITE, sql, Set.copyOf(reads), writes);
    }

    /** Postgres / MySQL 扫描器没有实现计划序列化，改从语句形状里取表。 */
    private static boolean unserializableScan(Map<String, Object> tree) {
        String type = text(tree.get("error_type")).toLowerCase(Locale.ROOT);
        String message = text(tree.get("error_message")).toLowerCase(Locale.ROOT);
        return "not implemented".equals(type) && message.contains("serialize");
    }

    private static Verdict writeFromShape(Connection connection, String sql) {
        WriteShape.Parsed parsed = WriteShape.parse(sql);
        Set<String> reads = new LinkedHashSet<>();
        if (parsed.query() != null && !parsed.query().isBlank()) {
            reads.addAll(selectCatalogs(connection, parsed.query()));
        }
        for (String extra : parsed.reads()) {
            if (!extra.isBlank()) {
                reads.addAll(selectCatalogs(connection, extra));
            }
        }
        Set<String> writes = parsed.targetCatalog().isBlank() ? Set.of() : Set.of(parsed.targetCatalog());
        if (writes.isEmpty() && reads.isEmpty()) {
            throw ApiException.bad("写语句没有明确的目标表");
        }
        return new Verdict(Mode.WRITE, sql, Set.copyOf(reads), writes);
    }

    private static Set<String> selectCatalogs(Connection connection, String query) {
        Map<String, Object> tree = parse(scalar(connection,
                "SELECT json_serialize_sql(?, skip_null := true, skip_empty := true)", query));
        if (isError(tree)) {
            throw ApiException.bad(limit("无法解析 SQL：" + text(tree.get("error_message"))));
        }
        return read(tree, query).readCatalogs();
    }

    private static void walkQuery(Map<String, Object> node, Set<String> outerCtes, Set<String> catalogs) {
        if (node.isEmpty()) {
            throw ApiException.bad("无法识别的查询结构");
        }
        Set<String> ctes = new HashSet<>(outerCtes);
        List<Object> cteEntries = list(object(node.get("cte_map")).get("map"));
        for (Object entry : cteEntries) {
            Map<String, Object> pair = object(entry);
            String name = text(pair.get("key"));
            if (!name.isEmpty()) {
                ctes.add(name);
            }
            Map<String, Object> value = object(pair.get("value"));
            walkQuery(object(value.get("query_node")), ctes, catalogs);
        }
        String type = text(node.get("type"));
        if ("SET_OPERATION_NODE".equals(type)) {
            for (Object child : list(node.get("children"))) {
                walkQuery(object(child), ctes, catalogs);
            }
            return;
        }
        if (!"SELECT_NODE".equals(type)) {
            throw ApiException.bad("无法识别的查询结构");
        }
        walkFrom(object(node.get("from_table")), ctes, catalogs);
    }

    private static void walkFrom(Map<String, Object> from, Set<String> ctes, Set<String> catalogs) {
        String type = text(from.get("type"));
        switch (type) {
            case "", "EMPTY" -> {
            }
            case "BASE_TABLE" -> {
                String table = text(from.get("table_name"));
                String catalog = text(from.get("catalog_name"));
                if (table.isEmpty()) {
                    throw ApiException.bad("查询里有无法识别的表");
                }
                if (catalog.isEmpty() && ctes.contains(table)) {
                    return;
                }
                if (!catalog.isEmpty()) {
                    catalogs.add(catalog);
                }
            }
            case "JOIN" -> {
                walkFrom(object(from.get("left")), ctes, catalogs);
                walkFrom(object(from.get("right")), ctes, catalogs);
            }
            case "SUBQUERY" -> walkQuery(object(firstPresent(from, "subquery", "query")), ctes, catalogs);
            case "TABLE_FUNCTION" -> throw ApiException.bad("查询里不能使用表函数");
            default -> throw ApiException.bad("无法识别的表引用");
        }
    }

    private static Set<String> targetCatalogs(Map<String, Object> plan) {
        String catalog = catalogName(object(plan.get("table_info")));
        if (catalog.isEmpty()) {
            return Set.of();
        }
        return Set.of(catalog);
    }

    private static void collectScans(Map<String, Object> node, Set<String> catalogs) {
        Map<String, Object> data = object(node.get("function_data"));
        String table = text(data.get("table"));
        String catalog = catalogName(data);
        if (!table.isEmpty() && !catalog.isEmpty()) {
            catalogs.add(catalog);
        }
        for (Object child : list(node.get("children"))) {
            collectScans(object(child), catalogs);
        }
    }

    private static String catalogName(Map<String, Object> holder) {
        String catalog = text(holder.get("catalog"));
        if (!catalog.isEmpty()) {
            return catalog;
        }
        List<Object> path = list(object(holder.get("qualified_name")).get("path"));
        if (path.size() >= 3) {
            return text(path.get(0));
        }
        return "";
    }

    private static boolean hasTable(Map<String, Object> node) {
        Map<String, Object> info = object(node.get("table_info"));
        if (!text(info.get("table")).isEmpty()) {
            return true;
        }
        Map<String, Object> data = object(node.get("function_data"));
        if (!text(data.get("table")).isEmpty()) {
            return true;
        }
        for (Object child : list(node.get("children"))) {
            if (hasTable(object(child))) {
                return true;
            }
        }
        return false;
    }

    private static void rejectFunctions(Object node) {
        if (node instanceof Map<?, ?> map) {
            String name = text(map.get("function_name")).toLowerCase(Locale.ROOT);
            if (BLOCKED_FUNCTIONS.contains(name)) {
                throw ApiException.bad("查询里不能使用 " + name);
            }
            Object path = object(map.get("qname")).get("path");
            for (Object item : list(path)) {
                String function = text(item).toLowerCase(Locale.ROOT);
                if (BLOCKED_FUNCTIONS.contains(function)) {
                    throw ApiException.bad("查询里不能使用 " + function);
                }
            }
            for (Object value : map.values()) {
                rejectFunctions(value);
            }
            return;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                rejectFunctions(item);
            }
        }
    }

    private static void ensureJson(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            try {
                statement.execute("LOAD json");
            } catch (SQLException ex) {
                statement.execute("INSTALL json");
                statement.execute("LOAD json");
            }
        } catch (SQLException ex) {
            throw ApiException.bad("查询解析不可用");
        }
    }

    private static String scalar(Connection connection, String query, String sql) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, sql);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw ApiException.bad("无法解析 SQL");
                }
                return rows.getString(1);
            }
        } catch (SQLException ex) {
            throw ApiException.bad(limit("无法解析 SQL：" + ex.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String json) {
        Object parsed = JsonParserFactory.getJsonParser().parseMap(json);
        if (!(parsed instanceof Map<?, ?> map)) {
            throw ApiException.bad("无法解析 SQL");
        }
        return (Map<String, Object>) map;
    }

    private static boolean isError(Map<String, Object> tree) {
        Object error = tree.get("error");
        return Boolean.TRUE.equals(error) || "true".equals(text(error));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static List<Object> list(Object value) {
        if (value instanceof List<?> list) {
            return List.copyOf(list);
        }
        return List.of();
    }

    private static Object firstPresent(Map<String, Object> node, String first, String second) {
        Object value = node.get(first);
        return value == null ? node.get(second) : value;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String limit(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= 300 ? message : message.substring(0, 300);
    }
}
