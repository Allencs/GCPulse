<template>
  <div v-if="stringDedup" class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><Operation /></el-icon>
      字符串去重统计
    </div>
    
    <div class="dedup-content">
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-icon">
            <el-icon :size="32" color="#409EFF"><View /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">检查的字符串总数</div>
            <div class="stat-value">{{ formatNumber(stringDedup.totalInspected) }}</div>
          </div>
        </div>
        
        <div class="stat-item">
          <div class="stat-icon">
            <el-icon :size="32" color="#67C23A"><Finished /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">去重的字符串总数</div>
            <div class="stat-value">{{ formatNumber(stringDedup.totalDeduplicated) }}</div>
          </div>
        </div>
        
        <div class="stat-item">
          <div class="stat-icon">
            <el-icon :size="32" color="#E6A23C"><PieChart /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">去重率</div>
            <div class="stat-value">{{ stringDedup.deduplicationRate?.toFixed(2) || '0.00' }}%</div>
          </div>
        </div>
        
        <div class="stat-item">
          <div class="stat-icon">
            <el-icon :size="32" color="#F56C6C"><Download /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">节省的字节数</div>
            <div class="stat-value">{{ formatBytes(stringDedup.bytesSaved) }}</div>
          </div>
        </div>
      </div>
      
      <div class="progress-section">
        <h4>去重效果</h4>
        <el-progress 
          :percentage="stringDedup.deduplicationRate || 0" 
          :color="getProgressColor(stringDedup.deduplicationRate)"
          :format="(percentage) => percentage.toFixed(2) + '%'"
        />
        <p class="progress-hint">
          通过字符串去重，共节省了 <strong>{{ formatBytes(stringDedup.bytesSaved) }}</strong> 的内存空间
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { View, Finished, PieChart, Download, Operation } from '@element-plus/icons-vue'

defineProps({
  stringDedup: {
    type: Object,
    default: () => null
  }
})

function formatNumber(num) {
  if (!num) return '0'
  return num.toLocaleString()
}

function formatBytes(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  if (bytes >= 1024 * 1024 * 1024) {
    return (bytes / (1024 * 1024 * 1024)).toFixed(3) + ' GB'
  } else if (bytes >= 1024 * 1024) {
    return (bytes / (1024 * 1024)).toFixed(3) + ' MB'
  } else if (bytes >= 1024) {
    return (bytes / 1024).toFixed(3) + ' KB'
  }
  return bytes + ' B'
}

function getProgressColor(percentage) {
  if (percentage >= 80) return '#67C23A'
  if (percentage >= 50) return '#E6A23C'
  return '#F56C6C'
}
</script>

<style lang="scss" scoped>
.dedup-content {
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
    margin-bottom: 30px;
    
    .stat-item {
      display: flex;
      align-items: center;
      padding: 20px;
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      
      .stat-icon {
        margin-right: 15px;
      }
      
      .stat-info {
        flex: 1;
        
        .stat-label {
          font-size: 13px;
          color: #606266;
          margin-bottom: 5px;
        }
        
        .stat-value {
          font-size: 22px;
          font-weight: bold;
          color: #303133;
        }
      }
    }
  }
  
  .progress-section {
    padding: 20px;
    background: #f9fafc;
    border-radius: 8px;
    
    h4 {
      margin-bottom: 15px;
      color: #303133;
      font-size: 15px;
    }
    
    .progress-hint {
      margin-top: 15px;
      font-size: 14px;
      color: #606266;
      text-align: center;
      
      strong {
        color: #409EFF;
      }
    }
  }
}
</style>

