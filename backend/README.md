# GCPulse - Backend

Java GC日志分析平台后端服务

## 技术栈

- Java 21
- Spring Boot 3.2.1
- Maven

## 功能特性

- 支持多种GC收集器日志解析（G1GC, ZGC, CMS, Parallel GC, Serial GC, Shenandoah）
- 实时GC性能指标分析
- 内存使用趋势分析
- 问题诊断和优化建议
- RESTful API接口

## 快速开始

### 环境要求

- JDK 21或更高版本
- Maven 3.6+

### 运行项目

```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动

### API接口

#### 1. 上传并分析GC日志

```
POST /api/gc/analyze
Content-Type: multipart/form-data

参数:
- file: GC日志文件
```

#### 2. 健康检查

```
GET /api/gc/health
```

#### 3. 获取支持的GC收集器

```
GET /api/gc/collectors
```

## 项目结构

```
backend/
├── src/main/java/com/gcpulse/
│   ├── GCPulseApplication.java         # 主应用类
│   ├── controller/                      # REST控制器
│   ├── service/                         # 业务服务
│   ├── parser/                          # GC日志解析器
│   ├── model/                           # 数据模型
│   └── config/                          # 配置类
├── src/main/resources/
│   └── application.yml                  # 应用配置
└── pom.xml                              # Maven配置
```

## 配置说明

在 `application.yml` 中可以配置:

- 服务器端口
- 文件上传限制
- CORS跨域设置
- 日志级别

## 许可证

Copyright © 2025

