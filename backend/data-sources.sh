#!/usr/bin/env bash
# 示例数据源：Postgres :5433、MySQL :3306、ClickHouse :8123/:9000、MinIO :9100。
# 和应用栈分开，./dev.sh 不会动这里。数据在命名卷里，down 不加 -v 会保留。
#
#   ./data-sources.sh          # up（默认）
#   ./data-sources.sh down
#   ./data-sources.sh restart
#   ./data-sources.sh logs [service]
#   ./data-sources.sh ps
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE="${COMPOSE:-docker-compose}"
# 文件里写了 name: lake-sources。再传一次，避免从别的目录启动时项目名被目录名带偏。
PROJECT="${COMPOSE_PROJECT_NAME:-lake-sources}"

cd "$DIR"
cmd="${1:-up}"
case "$cmd" in
  up)
    "$COMPOSE" -p "$PROJECT" -f docker-compose.datasources.yml up -d --wait
    printf '\nData sources ready:\n'
    printf '  postgres    127.0.0.1:5433  db=benchmark  benchmark/benchmark  table public.test1\n'
    printf '  mysql       127.0.0.1:3306  db=db1        demo/demo\n'
    printf '  clickhouse  127.0.0.1:8123  native :9000  db=demo  demo/demo\n'
    printf '  minio       127.0.0.1:9100  console :9001 minioadmin/minioadmin\n\n'
    ;;
  down)
    "$COMPOSE" -p "$PROJECT" -f docker-compose.datasources.yml down
    ;;
  restart)
    "$COMPOSE" -p "$PROJECT" -f docker-compose.datasources.yml restart
    ;;
  logs)
    shift
    "$COMPOSE" -p "$PROJECT" -f docker-compose.datasources.yml logs -f --tail 100 "$@"
    ;;
  ps)
    "$COMPOSE" -p "$PROJECT" -f docker-compose.datasources.yml ps
    ;;
  *)
    echo "usage: $0 [up|down|restart|logs|ps]  (default: up)" >&2
    exit 2
    ;;
esac
