# GC Analysis Platform - Frontend

Java GC日志分析平台前端应用

## 技术栈

- Vue 3
- Vite
- Element Plus
- ECharts
- Axios
- Vue Router

## 功能特性

- 拖拽上传GC日志文件
- 实时分析进度显示
- 交互式性能图表
- KPI关键指标展示
- 详细的统计分析
- 智能诊断和优化建议
- 现代化UI设计
- 响应式布局

## 快速开始

### 环境要求

- Node.js 18+
- npm 或 yarn

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

应用将在 `http://localhost:5173` 启动

### 构建生产版本

```bash
npm run build
```

构建产物将输出到 `dist` 目录

### 预览生产版本

```bash
npm run preview
```

## 项目结构

```
frontend/
├── public/                  # 静态资源
├── src/
│   ├── api/                # API接口
│   ├── assets/             # 资源文件
│   │   └── styles/        # 全局样式
│   ├── components/         # Vue组件
│   │   ├── FileUpload.vue
│   │   ├── KPIPanel.vue
│   │   ├── ChartsPanel.vue
│   │   ├── MemorySizeCard.vue
│   │   ├── PauseDurationCard.vue
│   │   ├── ObjectStatsCard.vue
│   │   ├── PhaseStatisticsCard.vue
│   │   └── DiagnosisPanel.vue
│   ├── views/              # 页面视图
│   │   ├── Home.vue
│   │   └── AnalysisResult.vue
│   ├── router/             # 路由配置
│   ├── App.vue             # 根组件
│   └── main.js             # 入口文件
├── index.html
├── vite.config.js          # Vite配置
└── package.json
```

## 配置说明

### API代理

在 `vite.config.js` 中配置了API代理，将 `/api` 请求代理到后端服务：

```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

### 后端服务

确保后端服务已启动在 `http://localhost:8080`

## 支持的浏览器

- Chrome (推荐)
- Firefox
- Safari
- Edge

## 许可证

Copyright © 2025

