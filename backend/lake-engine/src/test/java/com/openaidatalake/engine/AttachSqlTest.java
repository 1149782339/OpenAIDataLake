package com.openaidatalake.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachSqlTest {

    @Test
    void keepsPasswordOutOfTheAttachStatement() {
        ExternalSource source = new ExternalSource(7, "mysql_datasource", ExternalSource.Kind.MYSQL, "mysql", 3306, "demo", "db1");

        String secret = AttachSql.createSecret(source, "p'ass");
        assertTrue(secret.contains("PASSWORD 'p''ass'"));
        assertFalse(secret.contains("p'ass'"));

        String attach = AttachSql.attach(source);
        assertEquals("ATTACH '' AS \"mysql_datasource\" (TYPE mysql, SECRET \"src_7\", SCHEMA 'db1')", attach);
        assertFalse(attach.contains("p'ass"));
    }

    @Test
    void mapsClickHouseHttpPortToNative() {
        ExternalSource source = new ExternalSource(
                8, "clickhouse_datasource", ExternalSource.Kind.CLICKHOUSE, "clickhouse", 8123, "demo", "demo");

        String secret = AttachSql.createSecret(source, "demo");
        assertTrue(secret.contains("TYPE clickhouse"));
        assertTrue(secret.contains("PORT 9000"));
        assertFalse(secret.contains("PORT 8123"));
        assertEquals(
                "ATTACH '' AS \"clickhouse_datasource\" (TYPE clickhouse, SECRET \"src_8\", READ_ONLY)",
                AttachSql.attach(source));
    }
}
