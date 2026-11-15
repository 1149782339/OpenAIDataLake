#!/usr/bin/env bash
# 本机编译后启动应用栈。示例数据源不在这里启动：
#   ./data-sources.sh
#
#   ./dev.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE="${COMPOSE:-docker-compose}"
COMPOSE_FILES=(-f docker-compose.yml)

cd "$ROOT/backend"

log() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }

log "1/4  building jars (tests skipped)"
# Git Bash 注入的这两个变量会让 mvn 脚本解析路径失败。
unset MSYS_NO_PATHCONV MSYS2_ARG_CONV_EXCL || true
mvn -q -B -pl lake-console,s3-gateway -am -DskipTests package

log "2/4  building frontend bundle"
(cd "$ROOT/frontend" && npm run build)

log "3/4  refreshing the runtime image and restarting"
# Windows 上 Podman 的经典构建器经常不把 jar 的变更算进 COPY 缓存，
# 只加 --build 可能继续跑上一份 jar。运行镜像只做拷贝，--no-cache 仍然很快。
"$COMPOSE" "${COMPOSE_FILES[@]}" build --no-cache lake
"$COMPOSE" "${COMPOSE_FILES[@]}" up -d --wait

log "4/4  waiting for http://localhost"
for _ in $(seq 1 90); do
  # 经 nginx 探测。控制台在虚拟机里听 8080，Windows 上直接访问 127.0.0.1:8080 可能被拒绝。
  # --noproxy 避免本机 http_proxy 把这次请求送去 7890。
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 --noproxy '*' \
    http://127.0.0.1/api/auth/csrf || true)
  if [ "$code" = "200" ]; then
    printf '\nReady:  http://localhost\n\n'
    exit 0
  fi
  sleep 1
done

printf '\nAPI did not answer within 90s. Last service logs:\n\n'
"$COMPOSE" "${COMPOSE_FILES[@]}" logs --tail 60 lake
exit 1
