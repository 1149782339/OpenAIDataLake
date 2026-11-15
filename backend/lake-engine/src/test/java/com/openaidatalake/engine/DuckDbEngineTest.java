package com.openaidatalake.engine;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuckDbEngineTest {

    @Test
    void borrowedConnectionsShareTheDatabase() throws Exception {
        try (DuckDbEngine engine = new DuckDbEngine("jdbc:duckdb:memory:engineshare")) {
            try (Connection connection = engine.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE t(i INTEGER)");
                statement.execute("INSERT INTO t VALUES (1)");
            }
            try (Connection connection = engine.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT i FROM t")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
        }
    }
}
