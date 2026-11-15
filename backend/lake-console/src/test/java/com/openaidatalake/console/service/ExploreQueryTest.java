package com.openaidatalake.console.service;

import com.openaidatalake.engine.DuckDbEngine;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExploreQueryTest {

    @Test
    void runsASelectAndExplain() throws Exception {
        try (DuckDbEngine engine = new DuckDbEngine("jdbc:duckdb:memory:exploreselect")) {
            try (Connection connection = engine.getConnection()) {
                DataSourceService.Rows table = DataSourceService.run(connection, "select 1 as id", verdict -> {
                });
                assertEquals(java.util.List.of("id"), table.columns());
                assertEquals(1, table.rows().size());
                assertEquals(1, ((Number) table.rows().get(0).get(0)).intValue());
            }
            try (Connection connection = engine.getConnection()) {
                DataSourceService.Rows plan = DataSourceService.run(connection, "explain select 1", verdict -> {
                });
                assertFalse(plan.columns().isEmpty());
                assertFalse(plan.rows().isEmpty());
            }
        }
    }

    @Test
    void runsAnInsert() throws Exception {
        try (DuckDbEngine engine = new DuckDbEngine("jdbc:duckdb:memory:explorewrite")) {
            try (Connection connection = engine.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE t(i INTEGER)");
            }
            try (Connection connection = engine.getConnection()) {
                DataSourceService.Rows table = DataSourceService.run(connection, "INSERT INTO t VALUES (1)", verdict -> {
                });
                assertEquals(java.util.List.of("affected"), table.columns());
                assertEquals(1, ((Number) table.rows().get(0).get(0)).intValue());
            }
        }
    }
}
