package com.openaidatalake.console.service;

import com.openaidatalake.console.datasource.DataSourceFields;
import com.openaidatalake.console.datasource.JdbcSources;
import com.openaidatalake.console.datasource.SecretText;
import com.openaidatalake.console.query.SourceAccess;
import com.openaidatalake.console.query.SqlGate;
import com.openaidatalake.console.query.SqlGuard;
import com.openaidatalake.console.web.ApiException;
import com.openaidatalake.engine.DuckDbEngine;
import com.openaidatalake.engine.ExternalSource;
import com.openaidatalake.domain.crypto.AesTextCipher;
import com.openaidatalake.domain.datasource.DataSourceEngine;
import com.openaidatalake.domain.datasource.DataSourceGrant;
import com.openaidatalake.domain.datasource.DataSourceGrantKey;
import com.openaidatalake.domain.datasource.DataSourceGrantRepository;
import com.openaidatalake.domain.datasource.RegisteredDataSource;
import com.openaidatalake.domain.datasource.RegisteredDataSourceRepository;
import com.openaidatalake.domain.user.UserAccount;
import com.openaidatalake.domain.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class DataSourceService {

    private static final Logger log = LoggerFactory.getLogger(DataSourceService.class);

    private final RegisteredDataSourceRepository sources;
    private final DataSourceGrantRepository grants;
    private final UserAccountRepository users;
    private final AesTextCipher cipher;
    private final DuckDbEngine engine;
    private final DuckDbConnectionPool connections;

    public DataSourceService(RegisteredDataSourceRepository sources,
                             DataSourceGrantRepository grants,
                             UserAccountRepository users,
                             AesTextCipher cipher,
                             DuckDbEngine engine,
                             DuckDbConnectionPool connections) {
        this.sources = sources;
        this.grants = grants;
        this.users = users;
        this.cipher = cipher;
        this.engine = engine;
        this.connections = connections;
    }

    @Transactional(readOnly = true)
    public List<DataSourceView> list() {
        return sources.findAllByOrderByIdAsc().stream().map(DataSourceService::view).toList();
    }

    public void probe(String engineLabel, String host, Integer port, String user, String password, String database) {
        DataSourceEngine kind = DataSourceFields.engine(engineLabel);
        JdbcSources.probe(
                kind,
                DataSourceFields.host(host),
                DataSourceFields.port(port),
                DataSourceFields.user(user),
                DataSourceFields.password(password),
                DataSourceFields.database(database));
    }

    public DataSourceView create(String name, String engineLabel, String host, Integer port, String user, String password, String database, List<GrantInput> grantInputs) {
        String sourceName = DataSourceFields.name(name);
        DataSourceEngine kind = DataSourceFields.engine(engineLabel);
        String sourceHost = DataSourceFields.host(host);
        int sourcePort = DataSourceFields.port(port);
        String sourceUser = DataSourceFields.user(user);
        String sourcePassword = DataSourceFields.password(password);
        String sourceDatabase = DataSourceFields.database(database);
        if (sources.existsByNameIgnoreCase(sourceName)) {
            throw ApiException.conflict("数据源名称已存在");
        }
        JdbcSources.probe(kind, sourceHost, sourcePort, sourceUser, sourcePassword, sourceDatabase);

        RegisteredDataSource entity = new RegisteredDataSource();
        entity.setName(sourceName);
        entity.setEngine(kind);
        entity.setHost(sourceHost);
        entity.setPort(sourcePort);
        entity.setUsername(sourceUser);
        entity.setPassword(cipher.encrypt(sourcePassword));
        entity.setDatabaseName(sourceDatabase);
        RegisteredDataSource saved = sources.saveAndFlush(entity);
        try {
            saveGrants(saved, grantInputs);
            mount(saved, sourcePassword);
        } catch (RuntimeException ex) {
            sources.delete(saved);
            throw ex;
        }
        return view(saved);
    }

    @Transactional(readOnly = true)
    public List<GrantView> listGrants(long sourceId) {
        RegisteredDataSource source = sources.findById(sourceId).orElseThrow(() -> ApiException.notFound("数据源不存在"));
        return grantViews(source.getId());
    }

    @Transactional
    public List<GrantView> replaceGrants(long sourceId, List<GrantInput> grantInputs) {
        RegisteredDataSource source = sources.findById(sourceId).orElseThrow(() -> ApiException.notFound("数据源不存在"));
        grants.deleteAll(grants.findByIdSourceId(source.getId()));
        grants.flush();
        saveGrants(source, grantInputs);
        return grantViews(source.getId());
    }

    public List<SourceCatalog> catalog(String username) {
        UserAccount user = requireUser(username);
        Set<Long> allowed = new HashSet<>();
        for (DataSourceGrant grant : grants.findByIdUserId(user.getId())) {
            allowed.add(grant.getId().getSourceId());
        }
        List<SourceCatalog> catalog = new ArrayList<>();
        for (RegisteredDataSource source : sources.findAllByOrderByIdAsc()) {
            if (!allowed.contains(source.getId())) {
                continue;
            }
            try {
                List<SchemaView> schemas = JdbcSources.schemas(source, cipher.decrypt(source.getPassword())).stream()
                        .map(schema -> new SchemaView(schema.name(), schema.tables()))
                        .toList();
                catalog.add(new SourceCatalog(source.getName(), schemas, null));
            } catch (RuntimeException ex) {
                catalog.add(new SourceCatalog(source.getName(), List.of(), SecretText.limit(ex.getMessage() == null ? "无法读取数据源" : ex.getMessage())));
            }
        }
        return catalog;
    }

    public QueryView query(String sql, String username) {
        UserAccount user = requireUser(username);
        Map<String, SourceAccess.SourceRule> rules = new HashMap<>();
        for (RegisteredDataSource source : sources.findAllByOrderByIdAsc()) {
            rules.put(source.getName().toLowerCase(Locale.ROOT), new SourceAccess.SourceRule(
                    source.getId(),
                    source.getName(),
                    source.getEngine() == DataSourceEngine.CLICKHOUSE));
        }
        Map<Long, SourceAccess.GrantRule> held = new HashMap<>();
        for (DataSourceGrant grant : grants.findByIdUserId(user.getId())) {
            held.put(grant.getId().getSourceId(), new SourceAccess.GrantRule(grant.isWritable()));
        }
        long started = System.nanoTime();
        try (Connection connection = connections.getConnection()) {
            Rows table = run(connection, sql, verdict -> SourceAccess.check(verdict, rules, held));
            long elapsed = Math.max(0, (System.nanoTime() - started) / 1_000_000);
            return new QueryView(table.columns(), table.rows(), elapsed);
        } catch (SQLException ex) {
            throw ApiException.bad(SecretText.limit("查询失败：" + SecretText.redact(ex.getMessage(), null)));
        }
    }

    static Rows run(Connection connection, String sql, Consumer<SqlGate.Verdict> authorize) {
        try (Statement statement = connection.createStatement()) {
            try {
                statement.setQueryTimeout(30);
            } catch (SQLException ignored) {
                // 驱动不支持查询超时时，仍然用结果行数上限保护页面。
            }
            try {
                statement.setMaxRows(1000);
            } catch (SQLException ignored) {
                // 普通查询还会在 SQL 外包一层 LIMIT 1000。
            }
            SqlGate.Verdict verdict = SqlGate.inspect(connection, sql);
            authorize.accept(verdict);
            if (verdict.mode() == SqlGate.Mode.READ) {
                String executable = verdict.direct() ? verdict.sql() : SqlGuard.wrapSelect(verdict.sql());
                try (ResultSet rows = statement.executeQuery(executable)) {
                    return read(rows);
                }
            }
            boolean result = statement.execute(verdict.sql());
            if (result) {
                try (ResultSet rows = statement.getResultSet()) {
                    return read(rows);
                }
            }
            int affected = statement.getUpdateCount();
            return new Rows(List.of("affected"), List.of(List.of(Math.max(affected, 0))));
        } catch (SQLException ex) {
            throw ApiException.bad(SecretText.limit("查询失败：" + SecretText.redact(ex.getMessage(), null)));
        }
    }

    private static Rows read(ResultSet rows) throws SQLException {
        ResultSetMetaData meta = rows.getMetaData();
        int count = meta.getColumnCount();
        List<String> columns = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            columns.add(meta.getColumnLabel(index));
        }
        List<List<Object>> table = new ArrayList<>();
        while (rows.next()) {
            List<Object> row = new ArrayList<>(count);
            for (int index = 1; index <= count; index++) {
                row.add(cell(rows.getObject(index)));
            }
            table.add(row);
        }
        return new Rows(columns, table);
    }

    private static Object cell(Object value) {
        if (value == null
                || value instanceof Boolean
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Double) {
            return value;
        }
        if (value instanceof Short || value instanceof Byte) {
            return ((Number) value).intValue();
        }
        if (value instanceof Float number) {
            return number.doubleValue();
        }
        if (value instanceof BigInteger number) {
            return number.bitLength() < 63 ? number.longValue() : number.toString();
        }
        if (value instanceof BigDecimal number) {
            return number.toPlainString();
        }
        if (value instanceof byte[]) {
            return "<binary>";
        }
        return value.toString();
    }

    private void mount(RegisteredDataSource source, String password) {
        try {
            engine.attach(external(source), password);
            connections.attached();
        } catch (SQLException ex) {
            throw ApiException.bad(SecretText.limit("挂载数据源失败：" + SecretText.redact(ex.getMessage(), password)));
        }
    }

    private static ExternalSource external(RegisteredDataSource source) {
        ExternalSource.Kind kind = switch (source.getEngine()) {
            case POSTGRESQL -> ExternalSource.Kind.POSTGRESQL;
            case MYSQL -> ExternalSource.Kind.MYSQL;
            case CLICKHOUSE -> ExternalSource.Kind.CLICKHOUSE;
        };
        return new ExternalSource(
                source.getId(),
                source.getName(),
                kind,
                source.getHost(),
                source.getPort(),
                source.getUsername(),
                source.getDatabaseName());
    }

    private void saveGrants(RegisteredDataSource source, List<GrantInput> grantInputs) {
        if (grantInputs == null || grantInputs.isEmpty()) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        List<DataSourceGrant> rows = new ArrayList<>();
        for (GrantInput grant : grantInputs) {
            if (grant == null) {
                throw ApiException.bad("授权不正确");
            }
            if (!seen.add(grant.userId())) {
                throw ApiException.bad("同一个用户不能重复授权");
            }
            if (source.getEngine() == DataSourceEngine.CLICKHOUSE && grant.writable()) {
                throw ApiException.bad("ClickHouse 数据源不支持写入");
            }
            UserAccount account = users.findById(grant.userId()).orElseThrow(() -> ApiException.notFound("用户不存在"));
            if (!account.isEnabled()) {
                throw ApiException.bad("用户已停用");
            }
            rows.add(new DataSourceGrant(new DataSourceGrantKey(source.getId(), account.getId()), grant.writable()));
        }
        grants.saveAll(rows);
    }

    private List<GrantView> grantViews(long sourceId) {
        List<GrantView> views = new ArrayList<>();
        for (DataSourceGrant grant : grants.findByIdSourceId(sourceId)) {
            UserAccount account = users.findById(grant.getId().getUserId()).orElse(null);
            if (account == null) {
                continue;
            }
            views.add(new GrantView(account.getId(), account.getUsername(), grant.isWritable()));
        }
        return views;
    }

    private UserAccount requireUser(String username) {
        return users.findByUsername(username).orElseThrow(() -> ApiException.unauthorized("未登录"));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void attachRegistered() {
        Thread.startVirtualThread(() -> {
            for (RegisteredDataSource source : sources.findAllByOrderByIdAsc()) {
                try {
                    mount(source, cipher.decrypt(source.getPassword()));
                } catch (RuntimeException ex) {
                    log.warn("数据源 {} 挂载失败：{}", source.getName(), ex.getMessage());
                }
            }
        });
    }

    private static DataSourceView view(RegisteredDataSource source) {
        return new DataSourceView(
                source.getId(),
                source.getName(),
                source.getEngine().label(),
                source.getHost(),
                source.getPort(),
                source.getDatabaseName());
    }

    public record DataSourceView(long id, String name, String engine, String host, int port, String database) {
    }

    public record SchemaView(String name, List<String> tables) {
    }

    public record SourceCatalog(String name, List<SchemaView> schemas, String error) {
    }

    public record QueryView(List<String> columns, List<List<Object>> rows, long elapsed) {
    }

    record Rows(List<String> columns, List<List<Object>> rows) {
    }

    public record GrantInput(long userId, boolean writable) {
    }

    public record GrantView(long userId, String username, boolean writable) {
    }
}
