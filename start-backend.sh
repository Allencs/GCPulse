#!/bin/bash

# GCPulse 启动脚本
# 用于启动后端服务

echo "========================================="
echo "GCPulse 后端启动脚本"
echo "========================================="

# 设置Java环境
export JAVA_HOME=/Users/hb26933/Applications/jdk-21.0.5.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# 切换到backend目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/backend"

# 检查是否已编译
if [ ! -f "target/gcpulse-backend-1.0.0.jar" ]; then
    echo "未找到编译后的jar文件，正在编译..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "编译失败!"
        exit 1
    fi
fi

# 创建日志目录
mkdir -p logs

# 启动后端服务
echo ""
echo "========================================="
echo "启动GCPulse后端服务..."
echo "Java版本: $(java -version 2>&1 | head -n 1)"
echo "日志目录: $(pwd)/logs"
echo "========================================="
echo ""

java -jar target/gcpulse-backend-1.0.0.jar

echo ""
echo "========================================="
echo "服务已停止"
echo "========================================="

