CREATE TABLE IF NOT EXISTS test1 (
    id    BIGINT PRIMARY KEY,
    name  VARCHAR(64),
    score DOUBLE
);

INSERT IGNORE INTO test1 (id, name, score)
VALUES (1, 'alpha', 90.0),
       (2, 'beta', 84.5),
       (3, 'gamma', 73.0);
