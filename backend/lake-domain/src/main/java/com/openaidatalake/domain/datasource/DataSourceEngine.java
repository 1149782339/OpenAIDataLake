package com.openaidatalake.domain.datasource;

public enum DataSourceEngine {
    POSTGRESQL,
    MYSQL,
    CLICKHOUSE;

    public String label() {
        return switch (this) {
            case POSTGRESQL -> "PostgreSQL";
            case MYSQL -> "MySQL";
            case CLICKHOUSE -> "ClickHouse";
        };
    }

    public static DataSourceEngine fromLabel(String label) {
        return switch (label) {
            case "PostgreSQL" -> POSTGRESQL;
            case "MySQL" -> MYSQL;
            case "ClickHouse" -> CLICKHOUSE;
            default -> throw new IllegalArgumentException("数据源类型不正确");
        };
    }
}
