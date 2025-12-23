<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><Odometer /></el-icon>
      关键性能指标 (KPI)
    </div>
    
    <div class="kpi-grid">
      <!-- 吞吐量 -->
      <div class="kpi-item">
        <div class="kpi-header">
          <el-icon :size="24" color="#67C23A"><TrendCharts /></el-icon>
          <span class="kpi-label">吞吐量</span>
        </div>
        <div class="stat-number">{{ formatPercentage(kpiMetrics?.throughput) }}</div>
        <el-progress 
          :percentage="kpiMetrics?.throughput || 0" 
          :color="getThroughputColor(kpiMetrics?.throughput)"
          :show-text="false"
        />
        <p class="kpi-desc">应用运行时间占比</p>
      </div>
      
      <!-- 平均暂停时间 -->
      <div class="kpi-item">
        <div class="kpi-header">
          <el-icon :size="24" color="#409EFF"><Timer /></el-icon>
          <span class="kpi-label">平均暂停时间</span>
        </div>
        <div class="stat-number">{{ formatTime(kpiMetrics?.latency?.avgPauseTime) }}</div>
        <div class="kpi-range">
          <span>最小: {{ formatTime(kpiMetrics?.latency?.minPauseTime) }}</span>
          <span>最大: {{ formatTime(kpiMetrics?.latency?.maxPauseTime) }}</span>
        </div>
        <p class="kpi-desc">标准差: {{ formatTime(kpiMetrics?.latency?.stdDevPauseTime) }}</p>
      </div>
      
      <!-- 并发时间 -->
      <div class="kpi-item" v-if="kpiMetrics?.concurrentTime?.totalTime > 0">
        <div class="kpi-header">
          <el-icon :size="24" color="#E6A23C"><Clock /></el-icon>
          <span class="kpi-label">并发时间</span>
        </div>
        <div class="stat-number">{{ formatDuration(kpiMetrics?.concurrentTime?.totalTime) }}</div>
        <div class="kpi-range">
          <span>平均: {{ formatTime(kpiMetrics?.concurrentTime?.avgTime) }}</span>
        </div>
        <p class="kpi-desc">最大: {{ formatTime(kpiMetrics?.concurrentTime?.maxTime) }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Odometer, TrendCharts, Timer, Clock } from '@element-plus/icons-vue'

const props = defineProps({
  kpiMetrics: {
    type: Object,
    default: () => ({})
  }
})

function formatPercentage(value) {
  if (value === null || value === undefined) return 'N/A'
  return value.toFixed(3) + '%'
}

function formatTime(ms) {
  if (ms === null || ms === undefined) return 'N/A'
  if (ms < 1) {
    return (ms * 1000).toFixed(3) + ' μs'
  }
  return ms.toFixed(3) + ' ms'
}

function formatDuration(ms) {
  if (ms === null || ms === undefined) return 'N/A'
  
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  if (hours > 0) {
    return `${hours}h ${minutes % 60}m ${seconds % 60}s`
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`
  } else {
    return `${seconds}s`
  }
}

function getThroughputColor(throughput) {
  if (throughput >= 99) return '#67C23A'
  if (throughput >= 95) return '#E6A23C'
  return '#F56C6C'
}
</script>

<style lang="scss" scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
  
  .kpi-item {
    padding: 24px;
    background: linear-gradient(135deg, #f5f7fa 0%, #fff 100%);
    border-radius: 8px;
    border: 1px solid #e4e7ed;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }
    
    .kpi-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;
      
      .kpi-label {
        font-size: 15px;
        font-weight: 500;
        color: #606266;
      }
    }
    
    .stat-number {
      margin-bottom: 12px;
    }
    
    .el-progress {
      margin-bottom: 12px;
    }
    
    .kpi-range {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      color: #909399;
      margin-bottom: 8px;
    }
    
    .kpi-desc {
      font-size: 13px;
      color: #909399;
      margin: 0;
    }
  }
}

@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>

