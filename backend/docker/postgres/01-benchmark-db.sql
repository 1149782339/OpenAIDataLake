DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'benchmark') THEN
        CREATE ROLE benchmark LOGIN PASSWORD 'benchmark';
    END IF;
END
$$;

SELECT 'CREATE DATABASE benchmark OWNER benchmark'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'benchmark')\gexec

GRANT ALL PRIVILEGES ON DATABASE benchmark TO benchmark;
