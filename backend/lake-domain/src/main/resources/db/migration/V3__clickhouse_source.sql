ALTER TABLE data_sources DROP CONSTRAINT data_sources_engine_chk;
ALTER TABLE data_sources ADD CONSTRAINT data_sources_engine_chk
    CHECK (engine IN ('POSTGRESQL', 'MYSQL', 'CLICKHOUSE'));
