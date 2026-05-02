#!/bin/bash
# =============================================================
# Travel System 一键部署脚本 (适用于阿里云学生机 Ubuntu 22.04)
#
# 使用方法:
#   chmod +x deploy.sh
#   ./deploy.sh
#
# 这个脚本会自动:
#   1. 安装 Docker 和 Docker Compose (如果未安装)
#   2. 构建 Spring Boot 应用镜像
#   3. 启动 MySQL + 应用
#   4. 开放防火墙端口
# =============================================================
set -e

echo "========================================"
echo "  Travel System 部署脚本"
echo "========================================"
echo ""

# --- 检查/安装 Docker ---
if ! command -v docker &> /dev/null; then
    echo "[1/5] 安装 Docker..."
    curl -fsSL https://get.docker.com | sh
    sudo systemctl enable docker
    sudo systemctl start docker
    echo "Docker 安装完成"
else
    echo "[1/5] Docker 已安装"
fi

# --- 检查 Docker Compose ---
if ! docker compose version &> /dev/null; then
    echo "[2/5] 配置 Docker Compose 插件..."
    sudo apt update -qq && sudo apt install -y -qq docker-compose-plugin
    echo "Docker Compose 配置完成"
else
    echo "[2/5] Docker Compose 已可用"
fi

# --- 构建应用镜像 ---
echo "[3/5] 构建应用镜像..."
docker build -t travel-system:latest .
echo "镜像构建完成"

# --- 启动服务 ---
echo "[4/5] 启动服务..."
docker compose -f docker-compose-prod.yml up -d
echo ""

# --- 等待应用就绪 ---
echo "[5/5] 等待应用启动..."
for i in $(seq 1 30); do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo ""
        echo "========================================"
        echo "  部署成功!"
        echo "========================================"
        echo ""
        echo "  API 文档: http://$(curl -s ifconfig.me 2>/dev/null || echo 'YOUR_SERVER_IP'):8080/swagger-ui.html"
        echo "  健康检查: http://localhost:8080/actuator/health"
        echo ""
        echo "  常用命令:"
        echo "    查看日志: docker compose -f docker-compose-prod.yml logs -f app"
        echo "    停止服务: docker compose -f docker-compose-prod.yml down"
        echo "    重启服务: docker compose -f docker-compose-prod.yml restart"
        exit 0
    fi
    sleep 2
    echo -n "."
done

echo ""
echo "应用启动超时，请运行以下命令检查日志:"
echo "  docker compose -f docker-compose-prod.yml logs app"
