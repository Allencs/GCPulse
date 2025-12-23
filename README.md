# GCPulse - Java GC日志分析平台

一个专业的Java垃圾回收日志分析平台，帮助开发者快速诊断GC性能问题并提供优化建议。

## 📋 项目简介

GCPulse 是一个现代化的Web应用程序，用于分析Java应用程序的GC日志。它提供了直观的可视化界面、详细的性能指标分析以及智能的优化建议，帮助开发者优化JVM性能。

## ✨ 核心功能

### 1. 多格式支持
- ✅ G1GC
- ✅ ZGC
- ✅ CMS
- ✅ Parallel GC
- ✅ Serial GC
- ✅ Shenandoah

### 2. 关键性能指标 (KPI)
- 吞吐量分析
- 暂停时间统计（平均/最大/最小/标准差）
- 并发时间分析
- GC频率统计

### 3. 可视化分析
- 堆内存使用趋势图
- GC暂停时间分布图
- 时间序列数据展示
- 交互式图表

### 4. 详细统计
- JVM内存大小分析
- GC阶段统计
- 对象创建与晋升统计
- 暂停时间范围分布

### 5. 智能诊断
- 内存泄漏检测
- Full GC警告
- 长暂停识别
- 个性化优化建议

## 🏗️ 技术架构

### 后端
- **语言**: Java 21
- **框架**: Spring Boot 3.2.1
- **构建工具**: Maven
- **架构**: 单体应用，无数据存储

### 前端
- **框架**: Vue 3
- **构建工具**: Vite
- **UI库**: Element Plus
- **图表**: ECharts
- **样式**: SCSS

## 🚀 快速开始

### 环境要求
- JDK 21+
- Node.js 18+
- Maven 3.6+

### 1. 启动后端

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端应用将在 `http://localhost:5173` 启动

### 3. 使用应用

1. 在浏览器中打开 `http://localhost:5173`
2. 点击或拖拽GC日志文件到上传区域
3. 点击"开始分析"按钮
4. 等待分析完成，查看详细的分析报告

## 📁 项目结构

```
GCPulse/
├── backend/                          # 后端Java应用
│   ├── src/main/java/com/gcpulse/
│   │   ├── GCPulseApplication.java      # 主应用类
│   │   ├── controller/                  # REST控制器
│   │   ├── service/                     # 业务服务
│   │   ├── parser/                      # GC日志解析器
│   │   ├── model/                       # 数据模型
│   │   └── config/                      # 配置类
│   ├── src/main/resources/
│   │   └── application.yml             # 应用配置
│   └── pom.xml                         # Maven配置
│
├── frontend/                         # 前端Vue应用
│   ├── src/
│   │   ├── api/                         # API接口
│   │   ├── components/                  # Vue组件
│   │   ├── views/                       # 页面视图
│   │   ├── router/                      # 路由配置
│   │   ├── assets/                      # 资源文件
│   │   ├── App.vue                      # 根组件
│   │   └── main.js                      # 入口文件
│   ├── index.html
│   ├── vite.config.js                   # Vite配置
│   └── package.json
│
└── README.md                         # 项目说明
```

## 🎯 产品设计亮点

### 用户体验
- 拖拽上传，操作简单直观
- 实时进度显示，反馈及时
- 响应式设计，支持多种设备
- 平滑动画，提升交互体验

### UI设计
- 现代化渐变配色
- 清晰的信息层级
- 卡片式布局，信息分组明确
- 图表与数据结合，可视化效果好

### 架构设计
- 前后端分离，职责清晰
- RESTful API，标准化接口
- 无状态设计，易于扩展
- 模块化代码，易于维护

## 📊 分析报告示例

分析完成后，您将看到：

1. **文件信息概览** - 文件名、大小、GC收集器类型
2. **KPI指标面板** - 吞吐量、暂停时间、并发时间
3. **内存大小分析** - Heap、Metaspace使用情况
4. **交互式图表** - 堆内存趋势、暂停时间趋势
5. **暂停时间分布** - GC暂停时间范围统计
6. **对象统计** - 对象创建和晋升信息
7. **GC阶段统计** - 各阶段时间分布
8. **诊断报告** - 问题检测和优化建议

## 🔧 配置说明

### 后端配置 (application.yml)

```yaml
server:
  port: 8080

spring:
  servlet:
    multipart:
      max-file-size: 500MB      # 最大文件大小
      max-request-size: 500MB
```

### 前端配置 (vite.config.js)

```javascript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 🤝 贡献指南

欢迎贡献代码、提出问题或建议！

## 📄 许可证

Copyright © 2025 GCPulse. All rights reserved.

## 📞 联系方式

如有问题或建议，欢迎联系我们。

---

**享受使用 GCPulse！让GC性能优化变得简单！** 🎉

