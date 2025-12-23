<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><Setting /></el-icon>
      JVM 启动参数
    </div>
    
    <div v-if="jvmArguments && jvmArguments.allArguments && jvmArguments.allArguments.length > 0" class="jvm-args-content">
      <el-tabs v-model="activeTab" type="card">
        <el-tab-pane label="GC 相关" name="gc">
          <div class="args-list">
            <el-tag 
              v-for="(arg, index) in jvmArguments.gcArguments" 
              :key="index"
              class="arg-tag"
              type="success"
            >
              {{ arg }}
            </el-tag>
            <p v-if="!jvmArguments.gcArguments || jvmArguments.gcArguments.length === 0" class="no-data">
              未找到 GC 相关参数
            </p>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="内存相关" name="memory">
          <div class="args-list">
            <el-tag 
              v-for="(arg, index) in jvmArguments.memoryArguments" 
              :key="index"
              class="arg-tag"
              type="warning"
            >
              {{ arg }}
            </el-tag>
            <p v-if="!jvmArguments.memoryArguments || jvmArguments.memoryArguments.length === 0" class="no-data">
              未找到内存相关参数
            </p>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="性能相关" name="performance">
          <div class="args-list">
            <el-tag 
              v-for="(arg, index) in jvmArguments.performanceArguments" 
              :key="index"
              class="arg-tag"
              type="info"
            >
              {{ arg }}
            </el-tag>
            <p v-if="!jvmArguments.performanceArguments || jvmArguments.performanceArguments.length === 0" class="no-data">
              未找到性能相关参数
            </p>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="全部参数" name="all">
          <div class="args-list">
            <el-tag 
              v-for="(arg, index) in jvmArguments.allArguments" 
              :key="index"
              class="arg-tag"
            >
              {{ arg }}
            </el-tag>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    
    <el-empty v-else description="日志中未找到 JVM 参数信息" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Setting } from '@element-plus/icons-vue'

defineProps({
  jvmArguments: {
    type: Object,
    default: () => null
  }
})

const activeTab = ref('gc')
</script>

<style lang="scss" scoped>
.jvm-args-content {
  .args-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    
    .arg-tag {
      font-family: 'Courier New', monospace;
      font-size: 13px;
    }
    
    .no-data {
      color: #909399;
      font-style: italic;
    }
  }
}
</style>

