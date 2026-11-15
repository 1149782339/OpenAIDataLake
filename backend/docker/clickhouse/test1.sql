CREATE TABLE IF NOT EXISTS demo.test1 (
    id    UInt64,
    name  String,
    score Float64
) ENGINE = MergeTree
ORDER BY id;

INSERT INTO demo.test1 (id, name, score)
SELECT 1, 'alpha', 95
WHERE NOT EXISTS (SELECT 1 FROM demo.test1 WHERE id = 1);

INSERT INTO demo.test1 (id, name, score)
SELECT 2, 'beta', 88.5
WHERE NOT EXISTS (SELECT 1 FROM demo.test1 WHERE id = 2);

INSERT INTO demo.test1 (id, name, score)
SELECT 3, 'gamma', 80
WHERE NOT EXISTS (SELECT 1 FROM demo.test1 WHERE id = 3);
