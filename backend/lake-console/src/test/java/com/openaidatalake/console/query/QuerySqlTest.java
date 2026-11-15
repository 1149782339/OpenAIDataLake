package com.openaidatalake.console.query;

import com.openaidatalake.console.datasource.SecretText;
import com.openaidatalake.console.web.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuerySqlTest {

    @Test
    void wrapsASingleSelect() {
        String sql = SqlGuard.singleRead("select * from postgres_datasource.public.test1;");
        assertTrue(sql.startsWith("SELECT * FROM ("));
        assertTrue(sql.endsWith("LIMIT 1000"));
        assertTrue(sql.contains("postgres_datasource.public.test1"));
    }

    @Test
    void rejectsASecondStatementAndWrites() {
        assertThrows(ApiException.class, () -> SqlGuard.singleRead("select 1; drop table t"));
        assertThrows(ApiException.class, () -> SqlGuard.singleRead("delete from t"));
        assertThrows(ApiException.class, () -> SqlGuard.singleRead("select * from postgres_query('db', 'select 1')"));
    }

    @Test
    void redactsPasswordFromErrors() {
        assertEquals("连接失败：password=******", SecretText.redact("连接失败：password=demo", "demo"));
    }
}
