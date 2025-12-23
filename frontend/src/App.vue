<template>
  <div id="app" class="gc-analysis-app">
    <el-container>
      <el-header class="app-header">
        <div class="header-content">
          <div class="logo-section">
            <el-icon :size="32" color="#409EFF"><Monitor /></el-icon>
            <h1>GC Analysis Platform</h1>
          </div>
          <div class="header-subtitle">
            Java垃圾回收日志分析平台
          </div>
        </div>
      </el-header>
      
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
      
      <el-footer class="app-footer">
        <p>&copy; 2025 GC Analysis Platform. All rights reserved.</p>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup>
import { Monitor } from '@element-plus/icons-vue'
</script>

<style lang="scss">
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
}

.gc-analysis-app {
  min-height: 100vh;
}

.el-container {
  min-height: 100vh;
}

.app-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 40px;
  height: 80px !important;
  
  .header-content {
    text-align: center;
    
    .logo-section {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      
      h1 {
        font-size: 24px;
        font-weight: 700;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        margin: 0;
      }
    }
    
    .header-subtitle {
      font-size: 13px;
      color: #666;
      margin-top: 4px;
    }
  }
}

.app-main {
  padding: 40px 20px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

.app-footer {
  background: rgba(255, 255, 255, 0.9);
  text-align: center;
  padding: 20px;
  color: #666;
  font-size: 14px;
  height: 60px !important;
  
  p {
    margin: 0;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

