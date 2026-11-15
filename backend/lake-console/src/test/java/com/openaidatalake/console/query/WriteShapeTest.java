package com.openaidatalake.console.query;

import com.openaidatalake.console.web.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteShapeTest {

    @Test
    void splitsInsertSelect() {
        WriteShape.Parsed parsed = WriteShape.parse(
                "insert into postgres_benchmark.public.test1 select * from postgres_benchmark.public.test1");
        assertEquals("postgres_benchmark", parsed.targetCatalog());
        assertEquals("select * from postgres_benchmark.public.test1", parsed.query());
        assertTrue(parsed.reads().isEmpty());
    }

    @Test
    void keepsTheSelectAfterAColumnList() {
        WriteShape.Parsed parsed = WriteShape.parse(
                "insert into postgres_benchmark.public.test1 (id, name, score) select * from postgres_benchmark.public.test1");
        assertEquals("postgres_benchmark", parsed.targetCatalog());
        assertEquals("select * from postgres_benchmark.public.test1", parsed.query());
    }

    @Test
    void readsTheFromClauseOfAnUpdate() {
        WriteShape.Parsed parsed = WriteShape.parse(
                "update postgres_benchmark.public.test1 set name = name from mysql_db1.db1.t where t.id = test1.id");
        assertEquals("postgres_benchmark", parsed.targetCatalog());
        assertNull(parsed.query());
        assertEquals("SELECT * FROM mysql_db1.db1.t", parsed.reads().get(0).strip());
    }

    @Test
    void readsADeleteSubquery() {
        WriteShape.Parsed parsed = WriteShape.parse(
                "delete from postgres_benchmark.public.test1 where id in (select id from mysql_db1.db1.t)");
        assertEquals("postgres_benchmark", parsed.targetCatalog());
        assertEquals("select id from mysql_db1.db1.t", parsed.reads().get(0));
    }

    @Test
    void rejectsASecondStatement() {
        assertThrows(ApiException.class, () -> WriteShape.parse("insert into a.b.c select 1; select 2"));
    }
}
