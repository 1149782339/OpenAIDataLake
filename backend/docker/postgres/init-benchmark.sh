psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f /sql/01-benchmark-db.sql
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname benchmark -f /sql/02-benchmark-table.sql
