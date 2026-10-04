#!/usr/bin/env bash
# 社区图书漂流箱 —— 一键启动
# 用法: ./start.sh          构建前端 + 打包后端 + 启动（http://localhost:8080）
#       ./start.sh --skip-frontend   跳过前端构建（无 Node 环境时用已提交的产物）
set -euo pipefail
cd "$(dirname "$0")"

# ---- 定位 JDK 17+ ----
if ! command -v java >/dev/null 2>&1; then
  JDK_DIR=$(ls -d "$HOME"/tools/jdk-* 2>/dev/null | head -1 || true)
  if [ -n "$JDK_DIR" ]; then
    export JAVA_HOME="$JDK_DIR"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
fi
command -v java >/dev/null 2>&1 || { echo "错误: 未找到 Java，请安装 JDK 17+"; exit 1; }

# ---- 定位 Maven ----
if ! command -v mvn >/dev/null 2>&1; then
  MVN_DIR=$(ls -d "$HOME"/tools/apache-maven-* 2>/dev/null | head -1 || true)
  [ -n "$MVN_DIR" ] && export PATH="$MVN_DIR/bin:$PATH"
fi
command -v mvn >/dev/null 2>&1 || { echo "错误: 未找到 Maven，请安装 Maven 3.9+"; exit 1; }

# ---- 构建前端 ----
if [ "${1:-}" != "--skip-frontend" ]; then
  if command -v npm >/dev/null 2>&1; then
    echo "==> 构建前端"
    (cd frontend && npm install --no-fund --no-audit && npm run build)
  else
    echo "==> 未找到 npm，跳过前端构建"
  fi
fi

# ---- 打包并启动后端 ----
echo "==> 打包后端"
(cd backend && mvn -q -B package -DskipTests)

echo "==> 启动 http://localhost:8080 （Ctrl+C 停止）"
mkdir -p data/covers
exec java -jar backend/target/quarkus-app/quarkus-run.jar
