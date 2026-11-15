package com.openaidatalake.engine;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * 挂载外部数据源，并打开连到同一份内存库的连接。
 * 本身是一个 DataSource，连接池由调用方接上。不执行业务 SQL。
 */
public final class DuckDbEngine implements DataSource, AutoCloseable {

    static final String URL = "jdbc:duckdb:memory:lake";

    private final Object lock = new Object();
    private final String url;
    private Connection keeper;
    private PrintWriter logWriter;
    private int loginTimeout;
    private boolean extensionsReady;
    private boolean clickHouseReady;
    private boolean closed;

    public DuckDbEngine() {
        this(URL);
    }

    public DuckDbEngine(String url) {
        this.url = url;
    }

    public void attach(ExternalSource source, String password) throws SQLException {
        synchronized (lock) {
            if (closed) {
                throw new SQLException("查询引擎已关闭");
            }
            ensureExtensions(source.kind() == ExternalSource.Kind.CLICKHOUSE);
            Connection connection = keeper();
            try (Statement statement = connection.createStatement()) {
                statement.execute(AttachSql.createSecret(source, password));
                if (attached(connection, source.name())) {
                    statement.execute(AttachSql.detach(source.name()));
                }
                statement.execute(AttachSql.attach(source));
                clearCache(statement, source.kind());
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        synchronized (lock) {
            if (closed) {
                throw new SQLException("查询引擎已关闭");
            }
            keeper();
        }
        return DriverManager.getConnection(url);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) {
        this.loginTimeout = seconds;
    }

    @Override
    public int getLoginTimeout() {
        return loginTimeout;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("不能展开成 " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            if (keeper == null) {
                return;
            }
            try {
                keeper.close();
            } catch (SQLException ignored) {
                // 进程退出时连接关闭失败不影响已返回的结果。
            }
            keeper = null;
        }
    }

    private void ensureExtensions(boolean clickHouse) throws SQLException {
        if (!extensionsReady) {
            try (Statement statement = keeper().createStatement()) {
                statement.execute("INSTALL postgres");
                statement.execute("LOAD postgres");
                statement.execute("INSTALL mysql");
                statement.execute("LOAD mysql");
            }
            extensionsReady = true;
        }
        if (clickHouse && !clickHouseReady) {
            try (Statement statement = keeper().createStatement()) {
                statement.execute("INSTALL clickhouse_scanner FROM community");
                statement.execute("LOAD clickhouse_scanner");
            }
            clickHouseReady = true;
        }
    }

    private Connection keeper() throws SQLException {
        if (keeper == null || keeper.isClosed()) {
            ensureDriver();
            keeper = DriverManager.getConnection(url);
        }
        return keeper;
    }

    private static void ensureDriver() throws SQLException {
        try {
            Class.forName("org.duckdb.DuckDBDriver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("查询引擎不可用", ex);
        }
    }

    private static void clearCache(Statement statement, ExternalSource.Kind kind) {
        String sql = switch (kind) {
            case MYSQL -> "CALL mysql_clear_cache()";
            case CLICKHOUSE -> "CALL clickhouse_clear_cache()";
            case POSTGRESQL -> "CALL postgres_clear_cache()";
        };
        try {
            statement.execute(sql);
        } catch (SQLException ignored) {
            // 扩展还没提供清缓存函数时，挂载本身已经完成。
        }
    }

    private static boolean attached(Connection connection, String name) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT count(*) FROM duckdb_databases() WHERE lower(database_name) = lower(?)")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() && rows.getLong(1) > 0;
            }
        }
    }
}
