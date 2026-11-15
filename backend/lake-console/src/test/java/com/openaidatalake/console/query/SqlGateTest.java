package com.openaidatalake.console.query;

import com.openaidatalake.console.web.ApiException;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlGateTest {

    @Test
    void classifiesReadsAndRejectsBypassFunctions() throws Exception {
        withConnection(connection -> {
            SqlGate.Verdict select = SqlGate.inspect(connection, "SELECT a FROM catalog.schema.tbl");
            assertEquals(SqlGate.Mode.READ, select.mode());
            assertEquals(Set.of("catalog"), select.readCatalogs());
            assertTrue(select.writeCatalogs().isEmpty());

            SqlGate.Verdict explain = SqlGate.inspect(connection, "explain select * from postgres_benchmark.public.test1");
            assertEquals(SqlGate.Mode.READ, explain.mode());
            assertTrue(explain.direct());
            assertEquals(Set.of("postgres_benchmark"), explain.readCatalogs());

            SqlGate.Verdict union = SqlGate.inspect(connection,
                    "SELECT * FROM db.public.t1 UNION ALL SELECT * FROM db.public.t2");
            assertEquals(SqlGate.Mode.READ, union.mode());

            SqlGate.Verdict cte = SqlGate.inspect(connection,
                    "WITH c AS (SELECT * FROM db.public.secret) SELECT * FROM c");
            assertEquals(SqlGate.Mode.READ, cte.mode());
            assertEquals(Set.of("db"), cte.readCatalogs());

            SqlGate.Verdict join = SqlGate.inspect(connection,
                    "SELECT * FROM db.public.t1 JOIN db.public.t2 ON t1.id = t2.id");
            assertEquals(SqlGate.Mode.READ, join.mode());

            assertThrows(ApiException.class, () -> SqlGate.inspect(connection,
                    "SELECT * FROM postgres_query('db', 'select 1')"));
            assertThrows(ApiException.class, () -> SqlGate.inspect(connection, "SELECT 1; SELECT 2"));
        });
    }

    @Test
    void classifiesAnInsertSelectAgainstPostgres() throws Exception {
        Class.forName("org.duckdb.DuckDBDriver");
        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:memory:sqlgatepg");
             Statement statement = connection.createStatement()) {
            statement.execute("INSTALL postgres");
            statement.execute("LOAD postgres");
            statement.execute("""
                    CREATE SECRET pg (TYPE postgres, HOST '127.0.0.1', PORT 5433, DATABASE benchmark, USER 'benchmark', PASSWORD 'benchmark')
                    """);
            statement.execute("ATTACH '' AS postgres_benchmark (TYPE postgres, SECRET pg)");
            SqlGate.Verdict verdict = SqlGate.inspect(connection,
                    "insert into postgres_benchmark.public.test1 select * from postgres_benchmark.public.test1");
            assertEquals(SqlGate.Mode.WRITE, verdict.mode());
            assertEquals(Set.of("postgres_benchmark"), verdict.writeCatalogs());
            assertEquals(Set.of("postgres_benchmark"), verdict.readCatalogs());
        }
    }

    @Test
    void classifiesWritesAndRejectsDdl() throws Exception {
        withConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE t(i INTEGER)");
            }
            SqlGate.Verdict insert = SqlGate.inspect(connection, "INSERT INTO t VALUES (1)");
            assertEquals(SqlGate.Mode.WRITE, insert.mode());
            assertEquals(Set.of("memory"), insert.writeCatalogs());
            assertEquals(SqlGate.Mode.WRITE, SqlGate.inspect(connection, "UPDATE t SET i = 2 WHERE i = 1").mode());
            assertEquals(SqlGate.Mode.WRITE, SqlGate.inspect(connection, "DELETE FROM t WHERE i = 1").mode());
            assertThrows(ApiException.class, () -> SqlGate.inspect(connection, "DROP TABLE t"));
        });
    }

    private static void withConnection(SqlConsumer consumer) throws Exception {
        Class.forName("org.duckdb.DuckDBDriver");
        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:memory:sqlgate")) {
            consumer.accept(connection);
        }
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(Connection connection) throws Exception;
    }
}
