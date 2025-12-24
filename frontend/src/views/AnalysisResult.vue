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
      
      <!-- 连续 Full GC 警告（如果有） -->
      <ConsecutiveFullGCWarning 
        v-if="analysisData.diagnosisReport?.consecutiveFullGCInfo?.hasConsecutiveFullGC"
        :consecutive-full-gc-info="analysisData.diagnosisReport.consecutiveFullGCInfo" 
      />
      
      <!-- KPI指标面板 -->
      <KPIPanel :kpi-metrics="analysisData.kpiMetrics" />
      
      <!-- 详细 GC 统计 -->
      <ComprehensiveGCStats 
        :gc-events="analysisData.gcEvents"
        :kpi-metrics="analysisData.kpiMetrics"
      />
      
      <!-- JVM 参数（企业级功能） -->
      <JVMArgumentsCard 
        v-if="analysisData.jvmArguments"
        :jvm-arguments="analysisData.jvmArguments" 
      />
      
      <!-- 内存大小 -->
      <MemorySizeCard :memory-size="analysisData.memorySize" />
      
      <!-- 增强版交互式图表 -->
      <EnhancedChartsPanel 
        :time-series-data="analysisData.timeSeriesData" 
        :gc-events="analysisData.gcEvents"
      />
      
      <!-- GC原因统计（企业级功能） -->
      <GCCausesCard 
        v-if="analysisData.gcCauses && Object.keys(analysisData.gcCauses).length > 0"
        :gc-causes="analysisData.gcCauses" 
      />
      
      <!-- GC暂停时间分布 -->
      <PauseDurationCard :pause-distribution="analysisData.pauseDurationDistribution" />
      
      <!-- 对象统计 -->
      <ObjectStatsCard 
        :object-stats="analysisData.objectStats" 
        :collector-type="analysisData.collectorType" 
      />
      
      <!-- 老年代晋升总结（企业级功能） -->
      <TenuringSummaryCard 
        v-if="analysisData.tenuringSummary"
        :tenuring-summary="analysisData.tenuringSummary" 
      />
      
      <!-- 字符串去重统计（企业级功能） -->
      <StringDeduplicationCard 
        v-if="analysisData.stringDedup"
        :string-dedup="analysisData.stringDedup" 
      />
      
      <!-- GC阶段统计 -->
      <PhaseStatisticsCard :phase-statistics="analysisData.phaseStatistics" />
      
      <!-- 诊断报告 -->
      <DiagnosisPanel :diagnosis-report="analysisData.diagnosisReport" />
      
      <!-- AI智能诊断 -->
      <AIDiagnosis 
        :collector-type="analysisData.collectorType"
        :event-count="analysisData.gcEvents?.length || 0"
        :gc-log-file="gcLogFile"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Loading, Document, Setting, DataLine } from '@element-plus/icons-vue'
import KPIPanel from '../components/KPIPanel.vue'
import ComprehensiveGCStats from '../components/ComprehensiveGCStats.vue'
import MemorySizeCard from '../components/MemorySizeCard.vue'
import EnhancedChartsPanel from '../components/EnhancedChartsPanel.vue'
import PauseDurationCard from '../components/PauseDurationCard.vue'
import ObjectStatsCard from '../components/ObjectStatsCard.vue'
import PhaseStatisticsCard from '../components/PhaseStatisticsCard.vue'
import DiagnosisPanel from '../components/DiagnosisPanel.vue'
import AIDiagnosis from '../components/AIDiagnosis.vue'
// 企业级功能组件
import JVMArgumentsCard from '../components/JVMArgumentsCard.vue'
import GCCausesCard from '../components/GCCausesCard.vue'
import ConsecutiveFullGCWarning from '../components/ConsecutiveFullGCWarning.vue'
import TenuringSummaryCard from '../components/TenuringSummaryCard.vue'
import StringDeduplicationCard from '../components/StringDeduplicationCard.vue'

const router = useRouter()
const analysisData = ref(null)
const gcLogFile = ref(null)

onMounted(() => {
  // 从 Vue Router state 获取分析结果和原始文件
  if (window.history.state && window.history.state.analysisData) {
    analysisData.value = window.history.state.analysisData
    gcLogFile.value = window.history.state.gcLogFile || null
  } else {
    // 如果没有数据，返回首页
    router.push('/')
  }
})

function goBack() {
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

