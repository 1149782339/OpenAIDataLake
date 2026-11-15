package com.openaidatalake.engine;

final class AttachSql {

    private AttachSql() {
    }

    static String createSecret(ExternalSource source, String password) {
        String type = secretType(source.kind());
        int port = source.kind() == ExternalSource.Kind.CLICKHOUSE
                ? nativePort(source.port())
                : source.port();
        String timeout = source.kind() == ExternalSource.Kind.POSTGRESQL
                ? ",\n    CONNECT_TIMEOUT 5"
                : "";
        return """
                CREATE OR REPLACE SECRET %s (
                    TYPE %s,
                    HOST %s,
                    PORT %d,
                    DATABASE %s,
                    USER %s,
                    PASSWORD %s%s
                )
                """.formatted(
                quoteIdent(secretName(source.id())),
                type,
                quoteLiteral(source.host()),
                port,
                quoteLiteral(source.database()),
                quoteLiteral(source.username()),
                quoteLiteral(password),
                timeout);
    }

    static String detach(String name) {
        return "DETACH " + quoteIdent(name);
    }

    static String attach(ExternalSource source) {
        // ClickHouse 扫描扩展不能写，挂载保持只读。Postgres 和 MySQL 可写。
        String readOnly = source.kind() == ExternalSource.Kind.CLICKHOUSE ? ", READ_ONLY" : "";
        String schema = source.kind() == ExternalSource.Kind.MYSQL
                ? ", SCHEMA " + quoteLiteral(source.database())
                : "";
        return "ATTACH '' AS %s (TYPE %s, SECRET %s%s%s)".formatted(
                quoteIdent(source.name()),
                secretType(source.kind()),
                quoteIdent(secretName(source.id())),
                readOnly,
                schema);
    }

    /** ClickHouse 表单填 HTTP 8123 时，DuckDB 扩展走原生端口 9000。 */
    static int nativePort(int port) {
        if (port == 8123) {
            return 9000;
        }
        if (port == 8443) {
            return 9440;
        }
        return port;
    }

    private static String secretName(long id) {
        return "src_" + id;
    }

    private static String secretType(ExternalSource.Kind kind) {
        return switch (kind) {
            case MYSQL -> "mysql";
            case CLICKHOUSE -> "clickhouse";
            case POSTGRESQL -> "postgres";
        };
    }

    private static String quoteLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static String quoteIdent(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
