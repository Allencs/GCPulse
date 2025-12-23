#!/bin/bash

echo "========================================="
echo "测试 ZGC 日志解析"
echo "========================================="

# 检查后端是否运行
echo "检查后端服务..."
if ! curl -s http://localhost:8080/api/gc/health > /dev/null; then
    echo "❌ 后端服务未运行！请先启动: ./start-backend.sh"
    exit 1
fi

echo "✅ 后端服务正常运行"
echo ""

# 测试文件上传
echo "上传 GC 日志文件..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/gc/analyze \
  -F "file=@/Users/hb26933/Desktop/gc-2025-12-11_01-40-28.log")

echo "API 响应:"
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"

echo ""
echo "========================================="
echo "检查关键指标:"
echo "========================================="

# 提取关键信息
COLLECTOR=$(echo "$RESPONSE" | grep -o '"collectorType":"[^"]*"' | cut -d'"' -f4)
EVENT_COUNT=$(echo "$RESPONSE" | grep -o '"gcEvents":\[[^]]*\]' | grep -o 'timestamp' | wc -l | tr -d ' ')

echo "GC收集器类型: $COLLECTOR"
echo "GC事件数量: $EVENT_COUNT"

if [ "$COLLECTOR" = "ZGC" ] && [ "$EVENT_COUNT" -gt "0" ]; then
    echo ""
    echo "✅ 测试通过！ZGC 日志解析成功"
else
    echo ""
    echo "❌ 测试失败！"
    echo "   预期: collectorType=ZGC, gcEvents>0"
    echo "   实际: collectorType=$COLLECTOR, gcEvents=$EVENT_COUNT"
fi

