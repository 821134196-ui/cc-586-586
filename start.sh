#!/usr/bin/env bash
# 社区图书漂流箱系统 —— 一条命令启动
# 用法: ./start.sh        构建前端 + 打包后端 + 启动服务（http://localhost:8080）
#       ./start.sh test   只运行后端测试
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

# 使用仓库内置的 JDK / Maven（若系统已安装 java/mvn 则优先用系统的）
if [ -d "$ROOT/tools/jdk" ]; then
  export JAVA_HOME="$ROOT/tools/jdk"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
MVN="mvn"
if [ -x "$ROOT/tools/maven/bin/mvn" ]; then
  MVN="$ROOT/tools/maven/bin/mvn"
fi

cd "$ROOT/backend"

if [ "${1:-}" = "test" ]; then
  exec "$MVN" -B test
fi

echo "==> [1/3] 构建前端 (Svelte)"
cd "$ROOT/frontend"
if [ ! -d node_modules ]; then
  npm install --no-audit --no-fund
fi
npm run build

echo "==> [2/3] 打包后端 (Quarkus)"
cd "$ROOT/backend"
mkdir -p data/covers
"$MVN" -q -B package -DskipTests

echo "==> [3/3] 启动服务"
echo "    前端与管理后台: http://localhost:8080"
echo "    API 前缀:       http://localhost:8080/api"
echo "    SQLite 数据:    backend/data/bookbox.db"
echo "    按 Ctrl+C 停止"
exec "$JAVA_HOME/bin/java" -jar target/quarkus-app/quarkus-run.jar
