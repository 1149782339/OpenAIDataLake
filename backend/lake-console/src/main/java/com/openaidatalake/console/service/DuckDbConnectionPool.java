package com.openaidatalake.console.service;

import com.openaidatalake.engine.DuckDbEngine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Semaphore;

/**
 * 只负责借出和收回 DuckDB 连接。不调用 setNetworkTimeout 这类连接属性，嵌入式驱动没有这些方法。
 */
public final class DuckDbConnectionPool implements AutoCloseable {

    private static final int POOL_SIZE = 4;

    private final DuckDbEngine engine;
    private final Object lock = new Object();
    private final Semaphore slots = new Semaphore(POOL_SIZE);
    private final Deque<Connection> idle = new ArrayDeque<>();
    private volatile boolean attached;
    private volatile boolean closed;

    public DuckDbConnectionPool(DuckDbEngine engine) {
        this.engine = engine;
    }

    public void attached() {
        attached = true;
    }

    public Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("查询引擎已关闭");
        }
        boolean acquired = false;
        Connection raw = null;
        try {
            slots.acquire();
            acquired = true;
            synchronized (lock) {
                if (closed) {
                    throw new SQLException("查询引擎已关闭");
                }
                raw = takeIdle();
            }
            if (raw == null) {
                raw = engine.getConnection();
            }
            if (attached) {
                refresh(raw);
            }
            return lease(raw);
        } catch (SQLException ex) {
            discard(raw);
            if (acquired) {
                slots.release();
            }
            throw ex;
        } catch (InterruptedException ex) {
            discard(raw);
            if (acquired) {
                slots.release();
            }
            Thread.currentThread().interrupt();
            throw new SQLException("查询被中断", ex);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            while (!idle.isEmpty()) {
                discard(idle.removeFirst());
            }
        }
    }

    private Connection lease(Connection raw) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new Borrowed(raw));
    }

    private void recycle(Connection raw) {
        try {
            synchronized (lock) {
                if (closed || raw.isClosed()) {
                    discard(raw);
                } else {
                    idle.addLast(raw);
                }
            }
        } catch (SQLException ex) {
            discard(raw);
        } finally {
            slots.release();
        }
    }

    private Connection takeIdle() throws SQLException {
        while (!idle.isEmpty()) {
            Connection connection = idle.removeFirst();
            if (!connection.isClosed()) {
                return connection;
            }
        }
        return null;
    }

    private static void refresh(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            quiet(statement, "CALL postgres_clear_cache()");
            quiet(statement, "CALL mysql_clear_cache()");
            quiet(statement, "CALL clickhouse_clear_cache()");
        } catch (SQLException ignored) {
            // 扩展还没装时清缓存会失败，查询本身会返回具体错误。
        }
    }

    private static void quiet(Statement statement, String sql) {
        try {
            statement.execute(sql);
        } catch (SQLException ignored) {
            // 对应扩展未加载时跳过。
        }
    }

    private static void discard(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // 归还或关闭失败时丢掉这条连接。
        }
    }

    private final class Borrowed implements InvocationHandler {
        private final Connection raw;
        private boolean returned;

        private Borrowed(Connection raw) {
            this.raw = raw;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("close".equals(name)) {
                giveBack();
                return null;
            }
            if ("isClosed".equals(name)) {
                return returned;
            }
            if (returned) {
                throw new SQLException("Connection is closed");
            }
            try {
                return method.invoke(raw, args);
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause != null) {
                    throw cause;
                }
                throw ex;
            }
        }

        private void giveBack() {
            if (returned) {
                return;
            }
            returned = true;
            recycle(raw);
        }
    }
}
