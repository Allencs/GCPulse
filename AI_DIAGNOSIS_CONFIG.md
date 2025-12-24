# AI 诊断功能配置说明

## 概述

GCPulse 现已支持 **AI 智能诊断** 功能，可以调用大语言模型对 GC 日志进行深度分析。

支持以下 API 服务：
- ✅ **OpenAI 官方 API** (https://api.openai.com/v1)
- ✅ **OpenRouter** (https://openrouter.ai/api/v1) - 支持多种模型
- ✅ 其他兼容 OpenAI API 的服务

---

## 后端配置（推荐方式）

在 `backend/src/main/resources/application.yml` 中配置 AI 诊断参数：

```yaml
# AI诊断配置
ai:
  diagnosis:
    # API地址（支持OpenAI官方或兼容服务）
    api-url: https://openrouter.ai/api/v1  # 使用 OpenRouter
    # api-url: https://api.openai.com/v1  # 使用 OpenAI 官方
    
    # API Key（建议使用环境变量）
    api-key: ${AI_API_KEY:}  # 从环境变量 AI_API_KEY 读取，或直接填写
    # api-key: sk-or-v1-xxxxx  # 直接配置（不推荐，避免泄露）
    
    # 默认模型
    default-model: openai/gpt-4o  # OpenRouter 格式：provider/model-name
    # default-model: gpt-4o  # OpenAI 官方格式
    
    # 超时时间（秒）
    timeout-seconds: 60
    
    # 发送给AI的最大日志长度
    max-log-length: 15000
```

### 使用环境变量（推荐）

为了安全，建议使用环境变量设置 API Key：

**方式 1：在启动脚本中设置**
```bash
# 修改 start-backend.sh
export AI_API_KEY="your-api-key-here"
export JAVA_HOME=$JAVA_21_HOME
export PATH=$JAVA_HOME/bin:$PATH
mvn clean compile spring-boot:run
```

**方式 2：系统环境变量**
```bash
# 添加到 ~/.zshrc 或 ~/.bashrc
export AI_API_KEY="your-api-key-here"
```

---

## 前端使用

### 1. 使用后端配置（推荐）

如果已在后端 `application.yml` 中配置：
1. 上传 GC 日志文件
2. 在"诊断报告与优化建议"模块找到"AI智能诊断"
3. **直接点击"开始AI诊断"** - 无需填写任何配置
4. 等待 20-60 秒获取分析报告

### 2. 前端临时配置

如果没有配置后端，或需要使用不同的配置：
1. 填写 **API 地址**（如 `https://openrouter.ai/api/v1`）
2. 填写 **API Key**
3. 选择 **模型**（如 `openai/gpt-4o`）
4. 点击"开始AI诊断"

---

## OpenRouter 配置示例

### 获取 API Key
1. 访问 [OpenRouter](https://openrouter.ai)
2. 注册并登录
3. 在 [Keys](https://openrouter.ai/keys) 页面创建 API Key
4. 复制 Key（格式：`sk-or-v1-xxxxx`）

### 可用模型

OpenRouter 支持多种模型，格式为 `provider/model-name`：

**推荐模型（性能优秀）：**
- `openai/gpt-4o` - GPT-4o（最新、最强）
- `anthropic/claude-3.5-sonnet` - Claude 3.5 Sonnet（推理能力强）
- `openai/gpt-4-turbo` - GPT-4 Turbo（稳定）

**经济型模型：**
- `openai/gpt-3.5-turbo` - GPT-3.5 Turbo（快速、便宜）
- `anthropic/claude-3-haiku` - Claude 3 Haiku（快速）

更多模型请访问 [OpenRouter Models](https://openrouter.ai/models)

### 配置示例

```yaml
ai:
  diagnosis:
    api-url: https://openrouter.ai/api/v1
    api-key: sk-or-v1-your-key-here
    default-model: openai/gpt-4o  # 或 anthropic/claude-3.5-sonnet
    timeout-seconds: 60
    max-log-length: 15000
```

---

## OpenAI 官方 API 配置示例

### 获取 API Key
1. 访问 [OpenAI Platform](https://platform.openai.com)
2. 登录并创建 API Key
3. 复制 Key（格式：`sk-xxxxx`）

### 配置示例

```yaml
ai:
  diagnosis:
    api-url: https://api.openai.com/v1  # 可以留空，默认就是这个
    api-key: sk-your-openai-key-here
    default-model: gpt-4o  # 或 gpt-4, gpt-3.5-turbo
    timeout-seconds: 60
    max-log-length: 15000
```

---

## 常见问题

### Q1: 为什么建议使用 OpenRouter？
- ✅ 支持多种模型（OpenAI、Claude、Gemini 等）
- ✅ 价格更低
- ✅ 国内访问友好
- ✅ 统一的 API 接口

### Q2: API Key 会被存储吗？
- ❌ 不会！API Key 仅用于当前请求
- 前端输入的 Key 不会被存储
- 后端配置的 Key 存储在服务器本地

### Q3: 诊断需要多长时间？
- 首次诊断：20-60 秒
- 取决于模型速度和日志大小
- 建议使用 GPT-4o 或 Claude 3.5 Sonnet

### Q4: 如何选择模型？
- **最佳效果**：`openai/gpt-4o` 或 `anthropic/claude-3.5-sonnet`
- **经济实惠**：`openai/gpt-3.5-turbo`
- **快速诊断**：`anthropic/claude-3-haiku`

### Q5: 支持哪些 GC 收集器？
- ✅ G1GC
- ✅ ZGC（包括分代模式）
- ✅ CMS
- ✅ Parallel GC
- ✅ Serial GC

---

## 安全建议

1. **不要在代码中硬编码 API Key**
2. **使用环境变量存储敏感信息**
3. **定期轮换 API Key**
4. **限制 API Key 的权限和额度**
5. **不要将配置文件提交到公开仓库**

---

## 技术架构

```
前端 (Vue.js)
    ↓
后端 (Spring Boot)
    ↓
OpenAI SDK (支持自定义 baseUrl)
    ↓
API 服务 (OpenAI / OpenRouter / 其他)
    ↓
大语言模型 (GPT-4o / Claude 3.5 等)
```

---

## 更新日志

- **2025-12-25**: 
  - ✅ 支持 OpenRouter 和自定义 API 地址
  - ✅ 配置化管理（application.yml）
  - ✅ 支持环境变量配置 API Key
  - ✅ 前端 UI 优化
  - ✅ Markdown 渲染和代码高亮

---

## 联系支持

如有问题，请联系开发团队或查看项目文档。

