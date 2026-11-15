package com.openaidatalake.engine;

public record ExternalSource(
        long id,
        String name,
        Kind kind,
        String host,
        int port,
        String username,
        String database) {

    public enum Kind {
        POSTGRESQL,
        MYSQL,
        CLICKHOUSE
    }
}
