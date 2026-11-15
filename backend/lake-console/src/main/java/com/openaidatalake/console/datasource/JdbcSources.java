package com.openaidatalake.console.datasource;

import com.openaidatalake.console.web.ApiException;
import com.openaidatalake.domain.datasource.DataSourceEngine;
import com.openaidatalake.domain.datasource.RegisteredDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class JdbcSources {

    private JdbcSources() {
    }

    public static void probe(DataSourceEngine engine, String host, int port, String user, String password, String database) {
        if (engine == DataSourceEngine.CLICKHOUSE) {
            ClickHouseHttp.probe(host, port, user, password, database);
            return;
        }
        try (Connection connection = open(engine, host, port, user, password, database);
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(8);
            statement.execute("SELECT 1");
        } catch (SQLException ex) {
            throw ApiException.bad(SecretText.limit("连接失败：" + SecretText.redact(ex.getMessage(), password)));
        }
    }

    public static List<SchemaTables> schemas(RegisteredDataSource source, String password) {
        if (source.getEngine() == DataSourceEngine.CLICKHOUSE) {
            try {
                List<String> tables = ClickHouseHttp.tables(
                        source.getHost(),
                        source.getPort(),
                        source.getUsername(),
                        password,
                        source.getDatabaseName());
                if (tables.isEmpty()) {
                    return List.of();
                }
                return List.of(new SchemaTables(source.getDatabaseName(), tables));
            } catch (RuntimeException ex) {
                throw new SourceReadException(ex.getMessage() == null ? "无法读取 ClickHouse" : ex.getMessage());
            }
        }
        try (Connection connection = open(
                source.getEngine(),
                source.getHost(),
                source.getPort(),
                source.getUsername(),
                password,
                source.getDatabaseName())) {
            return source.getEngine() == DataSourceEngine.MYSQL
                    ? mysqlTables(connection, source.getDatabaseName())
                    : postgresTables(connection);
        } catch (SQLException ex) {
            throw new SourceReadException(SecretText.limit(SecretText.redact(ex.getMessage(), password)));
        }
    }

    private static Connection open(DataSourceEngine engine, String host, int port, String user, String password, String database)
            throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        String url = engine == DataSourceEngine.MYSQL
                ? "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?connectTimeout=5000&socketTimeout=8000&useSSL=false&allowPublicKeyRetrieval=true"
                : "jdbc:postgresql://" + host + ":" + port + "/" + database + "?connectTimeout=5&socketTimeout=8";
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(url, properties);
    }

    private static List<SchemaTables> postgresTables(Connection connection) throws SQLException {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        String sql = """
                SELECT n.nspname, c.relname
                FROM pg_catalog.pg_namespace n
                JOIN pg_catalog.pg_class c ON c.relnamespace = n.oid
                WHERE c.relkind IN ('r', 'v', 'm', 'p', 'f')
                  AND n.nspname <> 'pg_catalog'
                  AND n.nspname <> 'information_schema'
                  AND n.nspname NOT LIKE 'pg_toast%'
                  AND n.nspname NOT LIKE 'pg_temp%'
                  AND has_schema_privilege(n.oid, 'USAGE')
                  AND has_table_privilege(c.oid, 'SELECT')
                ORDER BY 1, 2
                """;
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(8);
            try (ResultSet rows = statement.executeQuery(sql)) {
                while (rows.next()) {
                    grouped.computeIfAbsent(rows.getString(1), key -> new ArrayList<>()).add(rows.getString(2));
                }
            }
        }
        return grouped.entrySet().stream()
                .map(entry -> new SchemaTables(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    private static List<SchemaTables> mysqlTables(Connection connection, String database) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ? AND table_type IN ('BASE TABLE', 'VIEW')
                ORDER BY table_name
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(8);
            statement.setString(1, database);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    tables.add(rows.getString(1));
                }
            }
        }
        if (tables.isEmpty()) {
            return List.of();
        }
        return List.of(new SchemaTables(database, List.copyOf(tables)));
    }

    public record SchemaTables(String name, List<String> tables) {
    }

    public static final class SourceReadException extends RuntimeException {
        public SourceReadException(String message) {
            super(message);
        }
    }
}
