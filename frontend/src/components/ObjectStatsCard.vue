<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><Box /></el-icon>
      对象统计
    </div>
    
    <div class="stats-grid">
      <div class="stat-item">
        <div class="stat-icon">
          <el-icon :size="40" color="#409EFF"><PieChart /></el-icon>
        </div>
        <div class="stat-content">
          <h4>总创建字节数</h4>
          <div class="stat-number">{{ objectStats?.totalCreatedFormatted || 'N/A' }}</div>
        </div>
      </div>
      
      <div class="stat-item">
        <div class="stat-icon">
          <el-icon :size="40" color="#67C23A"><TrendCharts /></el-icon>
        </div>
        <div class="stat-content">
          <h4>总晋升字节数</h4>
          <div class="stat-number">{{ getPromotedValue() }}</div>
        </div>
      </div>
      
      <div class="stat-item">
        <div class="stat-icon">
          <el-icon :size="40" color="#F56C6C"><Delete /></el-icon>
        </div>
        <div class="stat-content">
          <h4>总回收字节数</h4>
          <div class="stat-number">{{ objectStats?.totalReclaimedFormatted || 'N/A' }}</div>
        </div>
      </div>
      
      <div class="stat-item">
        <div class="stat-icon">
          <el-icon :size="40" color="#E6A23C"><Timer /></el-icon>
        </div>
        <div class="stat-content">
          <h4>平均创建速率</h4>
          <div class="stat-number">{{ formatRate(objectStats?.avgCreationRate) }}</div>
        </div>
      </div>
      
      <div class="stat-item" v-if="shouldShowPromotionRate()">
        <div class="stat-icon">
          <el-icon :size="40" color="#F56C6C"><TopRight /></el-icon>
        </div>
        <div class="stat-content">
          <h4>平均晋升速率</h4>
          <div class="stat-number">{{ formatRate(objectStats?.avgPromotionRate) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Box, PieChart, TrendCharts, Timer, TopRight, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  objectStats: {
    type: Object,
    default: () => ({})
  },
  collectorType: {
    type: String,
    default: ''
  }
})

function formatRate(rate) {
  if (rate === null || rate === undefined || rate === 0) return 'N/A'
  return rate.toFixed(3) + ' MB/s'
}

function getPromotedValue() {
  const promoted = props.objectStats?.totalPromotedFormatted
  // 如果显示 n/a，检查是否是单代模式
  if (!promoted || promoted === 'n/a') {
    if (props.collectorType === 'ZGC') {
      return '单代模式不适用'
    }
    return 'N/A'
  }
  return promoted
}

function shouldShowPromotionRate() {
  // 只有当晋升速率大于0时才显示（分代模式）
  return props.objectStats?.avgPromotionRate > 0
}
</script>

<style lang="scss" scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  
  .stat-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px;
    background: linear-gradient(135deg, #f5f7fa 0%, #fff 100%);
    border-radius: 8px;
    border: 1px solid #e4e7ed;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }
    
    .stat-icon {
      flex-shrink: 0;
    }
    
    .stat-content {
      flex: 1;
      
      h4 {
        font-size: 14px;
        font-weight: 500;
        color: #606266;
        margin: 0 0 8px 0;
      }
      
      .stat-number {
        font-size: 20px;
      }
    }
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>

