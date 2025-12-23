# 快速入门指南

## 前置条件

确保您的系统已安装以下软件：

- ✅ JDK 21 或更高版本
- ✅ Maven 3.6 或更高版本
- ✅ Node.js 18 或更高版本
- ✅ npm 或 yarn

## 验证环境

```bash
# 检查Java版本
java -version

# 检查Maven版本
mvn -version

# 检查Node版本
node -v

# 检查npm版本
npm -v
```

## 启动步骤

### 方式一：使用启动脚本（推荐）

#### 1. 启动后端服务

```bash
./start-backend.sh
```

后端服务将在 `http://localhost:8080` 启动

#### 2. 启动前端应用（新终端窗口）

```bash
./start-frontend.sh
```

前端应用将在 `http://localhost:5173` 启动

### 方式二：手动启动

#### 1. 启动后端

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

#### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

## 使用应用

1. 在浏览器中打开 `http://localhost:5173`

2. 您将看到欢迎页面，包含：
   - 产品特性介绍
   - 文件上传区域
   - 支持的GC收集器列表

3. 上传GC日志文件：
   - 点击上传区域选择文件，或直接拖拽文件到上传区域
   - 支持的文件格式：`.log`, `.txt`
   - 最大文件大小：500MB

4. 开始分析：
   - 点击"开始分析"按钮
   - 等待文件上传和分析完成
   - 系统会显示实时进度

5. 查看分析报告：
   - 文件信息概览
   - 关键性能指标（KPI）
   - JVM内存大小分析
   - 交互式图表（堆内存趋势、暂停时间趋势）
   - GC暂停时间分布
   - 对象统计
   - GC阶段统计
   - 诊断报告和优化建议

## 测试GC日志样例

如果您没有GC日志文件，可以使用以下JVM参数生成一个：

```bash
java -Xlog:gc*:file=gc.log -Xmx2g -XX:+UseG1GC YourApplication
```

或者对于旧版JVM：

```bash
java -Xloggc:gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xmx2g -XX:+UseG1GC YourApplication
```

## API接口测试

您也可以直接测试API接口：

### 健康检查

```bash
curl http://localhost:8080/api/gc/health
```

### 获取支持的GC收集器

```bash
curl http://localhost:8080/api/gc/collectors
```

### 上传并分析GC日志

```bash
curl -X POST http://localhost:8080/api/gc/analyze \
  -F "file=@/path/to/your/gc.log"
```

## 常见问题

### Q: 后端启动失败，提示端口被占用？

**A:** 检查8080端口是否被其他应用占用，可以修改 `backend/src/main/resources/application.yml` 中的端口配置。

### Q: 前端启动失败，提示端口被占用？

**A:** 检查5173端口是否被其他应用占用，可以修改 `frontend/vite.config.js` 中的端口配置。

### Q: 前端无法连接后端？

**A:** 确保：
1. 后端服务已正常启动
2. 后端运行在 `http://localhost:8080`
3. 检查浏览器控制台是否有CORS错误

### Q: 上传大文件失败？

**A:** 确保文件大小不超过500MB，如需支持更大文件，可以修改：
- 后端：`application.yml` 中的 `spring.servlet.multipart.max-file-size`
- 前端：`FileUpload.vue` 中的验证逻辑

### Q: 日志解析失败？

**A:** 可能的原因：
1. 日志格式不被支持
2. 日志文件损坏
3. 日志内容不完整

请确保使用标准的JVM GC日志格式。

## 生产环境部署

### 后端部署

```bash
cd backend
mvn clean package
java -jar target/gcpulse-backend-1.0.0.jar
```

### 前端部署

```bash
cd frontend
npm run build
```

构建产物在 `frontend/dist` 目录，可以部署到任何静态服务器（如Nginx）。

## 技术支持

如遇到问题，请查看：
- 项目根目录的 `README.md`
- 后端的 `backend/README.md`
- 前端的 `frontend/README.md`

或提交Issue反馈问题。

---

**祝您使用愉快！** 🎉

