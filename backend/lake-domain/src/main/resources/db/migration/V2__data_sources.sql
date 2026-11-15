CREATE TABLE data_sources (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(63)  NOT NULL,
    engine        VARCHAR(16)  NOT NULL,
    host          VARCHAR(255) NOT NULL,
    port          INTEGER      NOT NULL,
    username      VARCHAR(128) NOT NULL,
    password      VARCHAR(512) NOT NULL,
    database_name VARCHAR(128) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT data_sources_engine_chk CHECK (engine IN ('POSTGRESQL', 'MYSQL')),
    CONSTRAINT data_sources_port_chk CHECK (port BETWEEN 1 AND 65535)
);

CREATE UNIQUE INDEX data_sources_name_lower_idx ON data_sources (lower(name));
