CREATE TABLE data_source_grants (
    source_id  BIGINT  NOT NULL REFERENCES data_sources (id) ON DELETE CASCADE,
    user_id    BIGINT  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    writable   BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (source_id, user_id)
);

CREATE INDEX data_source_grants_user_idx ON data_source_grants (user_id);

-- 已有数据源先给启用的管理员授权，避免升级后立刻查不了。ClickHouse 只能读。
INSERT INTO data_source_grants (source_id, user_id, writable)
SELECT s.id, u.id, s.engine <> 'CLICKHOUSE'
FROM data_sources s
CROSS JOIN users u
WHERE u.role = 'ADMIN' AND u.enabled;
