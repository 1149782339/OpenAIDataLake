CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT users_role_chk CHECK (role IN ('ADMIN', 'USER'))
);

CREATE TABLE user_access_keys (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    access_key VARCHAR(128) NOT NULL UNIQUE,
    secret_key VARCHAR(512) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX user_access_keys_user_idx ON user_access_keys (user_id);

CREATE TABLE storage_backends (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(128) NOT NULL UNIQUE,
    provider   VARCHAR(16)  NOT NULL,
    endpoint   VARCHAR(512) NOT NULL,
    region     VARCHAR(64)  NOT NULL,
    path_style BOOLEAN      NOT NULL DEFAULT FALSE,
    access_key VARCHAR(256) NOT NULL,
    secret_key VARCHAR(512) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT storage_backends_provider_chk CHECK (provider IN ('MINIO', 'AWS', 'OSS'))
);

CREATE TABLE logical_paths (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bucket      VARCHAR(63)   NOT NULL,
    key_prefix  VARCHAR(1024) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT logical_paths_bucket_prefix_uk UNIQUE (bucket, key_prefix)
);

CREATE INDEX logical_paths_bucket_idx ON logical_paths (bucket);

CREATE TABLE path_routes (
    path_id          BIGINT PRIMARY KEY REFERENCES logical_paths (id) ON DELETE CASCADE,
    backend_id       BIGINT        NOT NULL REFERENCES storage_backends (id),
    physical_bucket  VARCHAR(63)   NOT NULL,
    physical_prefix  VARCHAR(1024) NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX path_routes_backend_idx ON path_routes (backend_id);

CREATE TABLE path_grants (
    path_id    BIGINT  NOT NULL REFERENCES logical_paths (id) ON DELETE CASCADE,
    user_id    BIGINT  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    can_list   BOOLEAN NOT NULL DEFAULT FALSE,
    can_read   BOOLEAN NOT NULL DEFAULT FALSE,
    can_write  BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (path_id, user_id),
    CONSTRAINT path_grants_action_chk CHECK (can_list OR can_read OR can_write OR can_delete)
);

CREATE INDEX path_grants_user_idx ON path_grants (user_id);

-- Spring Session JDBC。由 Flyway 建表，应用侧不再重复初始化。
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID            CHAR(36)     NOT NULL,
    SESSION_ID            CHAR(36)     NOT NULL,
    CREATION_TIME         BIGINT       NOT NULL,
    LAST_ACCESS_TIME      BIGINT       NOT NULL,
    MAX_INACTIVE_INTERVAL INT          NOT NULL,
    EXPIRY_TIME           BIGINT       NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BYTEA        NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);
