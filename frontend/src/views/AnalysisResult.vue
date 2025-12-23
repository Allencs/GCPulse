<template>
  <div class="analysis-result-view">
    <div v-if="!analysisData" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p class="loading-text">正在加载分析结果...</p>
    </div>
    
    <template v-else>
      <!-- 返回按钮 -->
      <el-button 
        class="back-btn" 
        :icon="ArrowLeft" 
        @click="goBack"
      >
        返回首页
      </el-button>
      
      <!-- 文件信息概览 -->
      <div class="analysis-card slide-in-up">
        <div class="file-overview">
          <div class="overview-item">
            <el-icon :size="32" color="#409EFF"><Document /></el-icon>
            <div class="overview-content">
              <h3>{{ analysisData.fileName }}</h3>
              <p>{{ formatFileSize(analysisData.fileSize) }}</p>
            </div>
          </div>
          <div class="overview-item">
            <el-icon :size="32" color="#67C23A"><Setting /></el-icon>
            <div class="overview-content">
              <h3>GC收集器</h3>
              <p>{{ analysisData.collectorType }}</p>
            </div>
          </div>
          <div class="overview-item">
            <el-icon :size="32" color="#E6A23C"><DataLine /></el-icon>
            <div class="overview-content">
              <h3>GC事件数</h3>
              <p>{{ analysisData.gcEvents?.length || 0 }} 次</p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- KPI指标面板 -->
      <KPIPanel :kpi-metrics="analysisData.kpiMetrics" />
      
      <!-- 内存大小 -->
      <MemorySizeCard :memory-size="analysisData.memorySize" />
      
      <!-- 交互式图表 -->
      <ChartsPanel :time-series-data="analysisData.timeSeriesData" />
      
      <!-- GC暂停时间分布 -->
      <PauseDurationCard :pause-distribution="analysisData.pauseDurationDistribution" />
      
      <!-- 对象统计 -->
      <ObjectStatsCard 
        :object-stats="analysisData.objectStats" 
        :collector-type="analysisData.collectorType" 
      />
      
      <!-- GC阶段统计 -->
      <PhaseStatisticsCard :phase-statistics="analysisData.phaseStatistics" />
      
      <!-- 诊断报告 -->
      <DiagnosisPanel :diagnosis-report="analysisData.diagnosisReport" />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Loading, Document, Setting, DataLine } from '@element-plus/icons-vue'
import KPIPanel from '../components/KPIPanel.vue'
import MemorySizeCard from '../components/MemorySizeCard.vue'
import ChartsPanel from '../components/ChartsPanel.vue'
import PauseDurationCard from '../components/PauseDurationCard.vue'
import ObjectStatsCard from '../components/ObjectStatsCard.vue'
import PhaseStatisticsCard from '../components/PhaseStatisticsCard.vue'
import DiagnosisPanel from '../components/DiagnosisPanel.vue'

const router = useRouter()
const analysisData = ref(null)

onMounted(() => {
  // 从sessionStorage获取分析结果
  const data = sessionStorage.getItem('gcAnalysisResult')
  if (data) {
    analysisData.value = JSON.parse(data)
  } else {
    // 如果没有数据，返回首页
    router.push('/')
  }
})

function goBack() {
  sessionStorage.removeItem('gcAnalysisResult')
  router.push('/')
}

function formatFileSize(bytes) {
  if (bytes >= 1024 * 1024 * 1024) {
    return (bytes / (1024 * 1024 * 1024)).toFixed(3) + ' GB'
  } else if (bytes >= 1024 * 1024) {
    return (bytes / (1024 * 1024)).toFixed(3) + ' MB'
  } else if (bytes >= 1024) {
    return (bytes / 1024).toFixed(3) + ' KB'
  }
  return bytes + ' B'
}
</script>

<style lang="scss" scoped>
.analysis-result-view {
  max-width: 1400px;
  margin: 0 auto;
  
  .back-btn {
    margin-bottom: 24px;
  }
  
  .file-overview {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 24px;
    
    .overview-item {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px;
      background: linear-gradient(135deg, #f5f7fa 0%, #fff 100%);
      border-radius: 8px;
      
      .overview-content {
        flex: 1;
        
        h3 {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 4px;
        }
        
        p {
          font-size: 14px;
          color: #606266;
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .file-overview {
    grid-template-columns: 1fr !important;
  }
}
</style>

