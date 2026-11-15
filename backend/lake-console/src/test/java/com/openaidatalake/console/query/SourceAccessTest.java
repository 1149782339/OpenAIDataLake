package com.openaidatalake.console.query;

import com.openaidatalake.console.web.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceAccessTest {

    private static final Map<String, SourceAccess.SourceRule> SOURCES = Map.of(
            "pg", new SourceAccess.SourceRule(1, "pg", false),
            "ch", new SourceAccess.SourceRule(2, "ch", true));

    @Test
    void readRequiresAGrantAndWriteRequiresWritable() {
        SqlGate.Verdict read = new SqlGate.Verdict(SqlGate.Mode.READ, "select", Set.of("pg"), Set.of());
        assertThrows(ApiException.class, () -> SourceAccess.check(read, SOURCES, Map.of()));
        assertDoesNotThrow(() -> SourceAccess.check(read, SOURCES, Map.of(1L, new SourceAccess.GrantRule(false))));

        SqlGate.Verdict write = new SqlGate.Verdict(SqlGate.Mode.WRITE, "insert", Set.of("pg"), Set.of("pg"));
        ApiException denied = assertThrows(ApiException.class,
                () -> SourceAccess.check(write, SOURCES, Map.of(1L, new SourceAccess.GrantRule(false))));
        assertEquals("没有 pg 的写权限", denied.getMessage());
        assertDoesNotThrow(() -> SourceAccess.check(write, SOURCES, Map.of(1L, new SourceAccess.GrantRule(true))));
    }

    @Test
    void clickHouseWriteIsRejectedEvenWithAGrant() {
        SqlGate.Verdict write = new SqlGate.Verdict(SqlGate.Mode.WRITE, "insert", Set.of(), Set.of("ch"));
        ApiException denied = assertThrows(ApiException.class,
                () -> SourceAccess.check(write, SOURCES, Map.of(2L, new SourceAccess.GrantRule(true))));
        assertEquals("ClickHouse 数据源不支持写入", denied.getMessage());
    }

    @Test
    void ignoresCatalogsThatAreNotDataSources() {
        SqlGate.Verdict local = new SqlGate.Verdict(SqlGate.Mode.WRITE, "insert", Set.of("memory"), Set.of("memory"));
        assertDoesNotThrow(() -> SourceAccess.check(local, SOURCES, Map.of()));
    }
}
