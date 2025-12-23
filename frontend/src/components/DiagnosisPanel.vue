<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><Warning /></el-icon>
      诊断报告与优化建议
    </div>
    
    <!-- 内存泄漏检测 -->
    <div class="diagnosis-section">
      <h3>
        <el-icon><Search /></el-icon>
        内存泄漏检测
      </h3>
      <el-alert
        :title="diagnosisReport?.memoryLeakInfo?.description || '未检测到明显的内存泄漏'"
        :type="diagnosisReport?.memoryLeakInfo?.hasMemoryLeak ? 'error' : 'success'"
        :closable="false"
        show-icon
      />
    </div>
    
    <!-- Full GC信息 -->
    <div class="diagnosis-section" v-if="diagnosisReport?.fullGCInfo">
      <h3>
        <el-icon><CircleClose /></el-icon>
        Full GC 检测
      </h3>
      <el-alert
        v-if="diagnosisReport.fullGCInfo.hasFullGC"
        :title="`检测到 ${diagnosisReport.fullGCInfo.count} 次 Full GC`"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>Full GC会导致应用完全停顿，建议优化内存配置或检查内存泄漏问题。</p>
        </template>
      </el-alert>
      <el-alert
        v-else
        title="未检测到 Full GC"
        type="success"
        :closable="false"
        show-icon
      />
    </div>
    
    <!-- 长暂停检测 -->
    <div class="diagnosis-section" v-if="diagnosisReport?.longPauseInfo">
      <h3>
        <el-icon><Clock /></el-icon>
        长暂停检测
      </h3>
      <el-alert
        v-if="diagnosisReport.longPauseInfo.hasLongPause"
        :title="`检测到 ${diagnosisReport.longPauseInfo.count} 次长暂停 (>${diagnosisReport.longPauseInfo.threshold}ms)`"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>长时间的GC暂停会影响应用响应时间，建议考虑使用低延迟GC收集器。</p>
        </template>
      </el-alert>
      <el-alert
        v-else
        title="未检测到长暂停"
        type="success"
        :closable="false"
        show-icon
      />
    </div>
    
    <!-- 优化建议 -->
    <div class="diagnosis-section">
      <h3>
        <el-icon><Tickets /></el-icon>
        优化建议
      </h3>
      <div class="recommendations-list">
        <el-card
          v-for="(rec, index) in diagnosisReport?.recommendations || []"
          :key="index"
          class="recommendation-card"
          :class="rec.level.toLowerCase()"
          shadow="hover"
        >
          <div class="rec-header">
            <el-tag :type="getTagType(rec.level)" size="small">
              {{ rec.level }}
            </el-tag>
            <span class="rec-category">{{ rec.category }}</span>
          </div>
          <h4>{{ rec.title }}</h4>
          <p class="rec-description">{{ rec.description }}</p>
          <div class="rec-suggestion">
            <strong>建议：</strong>
            <p>{{ rec.suggestion }}</p>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Warning, Search, CircleClose, Clock, Tickets } from '@element-plus/icons-vue'

const props = defineProps({
  diagnosisReport: {
    type: Object,
    default: () => ({})
  }
})

function getTagType(level) {
  const map = {
    'CRITICAL': 'danger',
    'WARNING': 'warning',
    'INFO': 'info'
  }
  return map[level] || 'info'
}
</script>

<style lang="scss" scoped>
.diagnosis-section {
  margin-bottom: 32px;
  
  h3 {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 16px;
    
    .el-icon {
      font-size: 18px;
      color: #409EFF;
    }
  }
  
  .el-alert {
    margin-bottom: 12px;
  }
}

.recommendations-list {
  display: grid;
  gap: 16px;
  
  .recommendation-card {
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
    }
    
    &.critical {
      border-left: 4px solid #F56C6C;
    }
    
    &.warning {
      border-left: 4px solid #E6A23C;
    }
    
    &.info {
      border-left: 4px solid #409EFF;
    }
    
    .rec-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;
      
      .rec-category {
        font-size: 13px;
        color: #909399;
      }
    }
    
    h4 {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
      margin: 0 0 12px 0;
    }
    
    .rec-description {
      font-size: 14px;
      color: #606266;
      margin-bottom: 12px;
      line-height: 1.6;
    }
    
    .rec-suggestion {
      padding: 12px;
      background: #f5f7fa;
      border-radius: 4px;
      font-size: 13px;
      
      strong {
        color: #409EFF;
        margin-bottom: 4px;
        display: block;
      }
      
      p {
        margin: 0;
        color: #606266;
        line-height: 1.6;
      }
    }
  }
}
</style>

