<template>
  <div class="analysis-result-view">
    <div v-if="!analysisData" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p class="loading-text">正在加载分析结果...</p>
    </div>
    
    <template v-else>
      <!-- 返回按钮和导出按钮 -->
      <div class="top-actions">
        <el-button 
          class="back-btn" 
          :icon="ArrowLeft" 
          @click="goBack"
        >
          返回首页
        </el-button>
        <el-button 
          v-if="activeTab === 'analysis'"
          type="primary" 
          :icon="Download" 
          @click="exportToPdf"
          :loading="exportingPdf"
        >
          导出为PDF
        </el-button>
      </div>
      
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
      
      <!-- Tab 切换区域 -->
      <div class="tabs-container">
        <el-tabs v-model="activeTab" class="result-tabs" @tab-click="handleTabClick">
          <el-tab-pane label="分析结果" name="analysis">
            <template #label>
              <span class="tab-label">
                <el-icon><DataAnalysis /></el-icon>
                分析结果
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="AI诊断" name="ai">
            <template #label>
              <span class="tab-label">
                <el-icon><MagicStick /></el-icon>
                AI智能诊断
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>
      
      <!-- 分析结果Tab内容 -->
      <div v-show="activeTab === 'analysis'" class="tab-content" ref="analysisContent">
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
      </div>
      
      <!-- AI诊断Tab内容 -->
      <div v-show="activeTab === 'ai'" class="tab-content">
        <AIDiagnosis 
          :collector-type="analysisData.collectorType"
          :event-count="analysisData.gcEvents?.length || 0"
          :gc-log-file="gcLogFile"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Loading, Document, Setting, DataLine, DataAnalysis, MagicStick, Download } from '@element-plus/icons-vue'
import { ElMessage, ElLoading } from 'element-plus'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
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
const activeTab = ref('analysis')
const analysisContent = ref(null)
const exportingPdf = ref(false)

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

function handleTabClick(tab) {
  // 平滑滚动到顶部
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
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

// 导出分析结果为PDF（截图方式）
async function exportToPdf() {
  if (!analysisData.value) {
    ElMessage.warning('没有可导出的分析结果')
    return
  }

  exportingPdf.value = true
  let loadingInstance = null
  
  try {
    loadingInstance = ElLoading.service({
      lock: true,
      text: '正在生成PDF报告，请稍候...',
      background: 'rgba(0, 0, 0, 0.7)'
    })

    // 创建PDF文档 (A4尺寸)
    const pdf = new jsPDF('p', 'mm', 'a4')
    const pageWidth = pdf.internal.pageSize.getWidth()
    const pageHeight = pdf.internal.pageSize.getHeight()
    const margin = 10
    const contentWidth = pageWidth - 2 * margin
    let yOffset = margin

    // 1. 添加封面
    pdf.setFontSize(24)
    pdf.setTextColor(102, 126, 234)
    pdf.text('GCPulse 分析报告', pageWidth / 2, 40, { align: 'center' })
    
    pdf.setFontSize(12)
    pdf.setTextColor(0, 0, 0)
    pdf.text(`GC收集器: ${analysisData.value.collectorType}`, pageWidth / 2, 60, { align: 'center' })
    pdf.text(`GC事件数: ${analysisData.value.gcEvents?.length || 0}`, pageWidth / 2, 70, { align: 'center' })
    pdf.text(`生成时间: ${new Date().toLocaleString('zh-CN')}`, pageWidth / 2, 80, { align: 'center' })

    // 等待页面完全渲染
    await nextTick()
    await new Promise(resolve => setTimeout(resolve, 500))

    // 2. 截取文件概览
    const fileOverview = document.querySelector('.file-overview')
    if (fileOverview) {
      pdf.addPage()
      pdf.setFontSize(16)
      pdf.setTextColor(64, 158, 255)
      pdf.text('📋 文件概览', margin, 20)
      
      const canvas = await html2canvas(fileOverview, {
        scale: 2,
        logging: false,
        useCORS: true
      })
      const imgData = canvas.toDataURL('image/png')
      const imgHeight = (canvas.height * contentWidth) / canvas.width
      pdf.addImage(imgData, 'PNG', margin, 30, contentWidth, imgHeight)
    }

    // 3. 截取KPI指标面板
    loadingInstance.text = '正在截取KPI指标...'
    const kpiPanel = document.querySelector('.kpi-panel')
    if (kpiPanel) {
      pdf.addPage()
      pdf.setFontSize(16)
      pdf.setTextColor(64, 158, 255)
      pdf.text('📊 KPI指标', margin, 20)
      
      const canvas = await html2canvas(kpiPanel, {
        scale: 2,
        logging: false,
        useCORS: true
      })
      const imgData = canvas.toDataURL('image/png')
      const imgHeight = (canvas.height * contentWidth) / canvas.width
      pdf.addImage(imgData, 'PNG', margin, 30, contentWidth, imgHeight)
    }

    // 4. 截取交互式图表的各个视图
    loadingInstance.text = '正在截取交互式图表...'
    const chartViews = [
      { name: 'heapAfter', label: 'Heap After GC' },
      { name: 'heapBefore', label: 'Heap Before GC' },
      { name: 'duration', label: 'GC Duration' },
      { name: 'reclaimed', label: 'Reclaimed Bytes' },
      { name: 'youngGen', label: 'Young Generation' },
      { name: 'oldGen', label: 'Old Generation' },
      { name: 'allocation', label: 'Allocation & Promotion' },
      { name: 'metaspace', label: 'Metaspace' }
    ]

    const chartPanel = document.querySelector('.chart-container')
    if (chartPanel) {
      for (const view of chartViews) {
        // 检查按钮是否可用
        const button = document.querySelector(`[data-view="${view.name}"]`) || 
                      Array.from(document.querySelectorAll('.view-selector button'))
                        .find(btn => btn.textContent.trim().includes(view.label.split(' ')[0]))
        
        if (button && !button.disabled) {
          // 切换到对应视图
          button.click()
          await nextTick()
          await new Promise(resolve => setTimeout(resolve, 800)) // 等待图表渲染
          
          // 截图
          pdf.addPage()
          pdf.setFontSize(16)
          pdf.setTextColor(64, 158, 255)
          pdf.text(`📈 ${view.label}`, margin, 20)
          
          const canvas = await html2canvas(chartPanel, {
            scale: 2,
            logging: false,
            useCORS: true,
            backgroundColor: '#ffffff'
          })
          const imgData = canvas.toDataURL('image/png')
          const imgHeight = (canvas.height * contentWidth) / canvas.width
          
          // 如果图表太高，分页显示
          if (imgHeight > pageHeight - 40) {
            const scaledHeight = pageHeight - 40
            pdf.addImage(imgData, 'PNG', margin, 30, contentWidth, scaledHeight)
          } else {
            pdf.addImage(imgData, 'PNG', margin, 30, contentWidth, imgHeight)
          }
          
          loadingInstance.text = `正在截取: ${view.label}...`
        }
      }
    }

    // 5. 截取GC统计信息（在图表下方）
    const gcStats = document.querySelector('.gc-statistics')
    if (gcStats) {
      pdf.addPage()
      pdf.setFontSize(16)
      pdf.setTextColor(64, 158, 255)
      pdf.text('📊 GC统计信息', margin, 20)
      
      const canvas = await html2canvas(gcStats, {
        scale: 2,
        logging: false,
        useCORS: true
      })
      const imgData = canvas.toDataURL('image/png')
      const imgHeight = (canvas.height * contentWidth) / canvas.width
      pdf.addImage(imgData, 'PNG', margin, 30, contentWidth, imgHeight)
    }

    // 6. 截取其他重要卡片
    loadingInstance.text = '正在截取统计卡片...'
    const cards = document.querySelectorAll('.analysis-card')
    for (let i = 0; i < cards.length; i++) {
      const card = cards[i]
      // 跳过图表面板（已经处理）
      if (card.querySelector('.chart-container')) continue
      
      try {
        const canvas = await html2canvas(card, {
          scale: 2,
          logging: false,
          useCORS: true
        })
        
        const imgData = canvas.toDataURL('image/png')
        const imgHeight = (canvas.height * contentWidth) / canvas.width
        
        // 检查是否需要新页
        if (imgHeight > pageHeight - 40) {
          pdf.addPage()
          const scaledHeight = pageHeight - 40
          pdf.addImage(imgData, 'PNG', margin, 20, contentWidth, scaledHeight)
        } else {
          pdf.addPage()
          pdf.addImage(imgData, 'PNG', margin, 20, contentWidth, imgHeight)
        }
      } catch (err) {
        console.warn('截取卡片失败:', err)
      }
    }

    // 保存PDF
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5)
    const fileName = `GCPulse_Analysis_${analysisData.value.collectorType}_${timestamp}.pdf`
    pdf.save(fileName)

    loadingInstance.close()
    ElMessage.success('PDF报告已成功生成！')
  } catch (err) {
    console.error('导出PDF失败:', err)
    if (loadingInstance) loadingInstance.close()
    ElMessage.error(`导出PDF失败: ${err.message || '未知错误'}`)
  } finally {
    exportingPdf.value = false
  }
}

</script>

<style lang="scss" scoped>
.analysis-result-view {
  max-width: 1400px;
  margin: 0 auto;
  
  .top-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    gap: 16px;
    flex-wrap: wrap;
  }
  
  .back-btn {
    // margin-bottom: 24px;
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
  
  .tabs-container {
    margin: 24px 0;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    padding: 0;
    overflow: hidden;
    
    :deep(.result-tabs) {
      .el-tabs__header {
        margin: 0;
        border-bottom: 2px solid #f0f0f0;
        background: linear-gradient(135deg, #f8f9fa 0%, #fff 100%);
      }
      
      .el-tabs__nav-wrap::after {
        display: none;
      }
      
      .el-tabs__item {
        height: 60px;
        line-height: 60px;
        font-size: 16px;
        font-weight: 500;
        color: #606266;
        padding: 0 32px;
        transition: all 0.3s ease;
        
        &:hover {
          color: #409EFF;
          background: rgba(64, 158, 255, 0.05);
        }
        
        &.is-active {
          color: #409EFF;
          background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(102, 126, 234, 0.1) 100%);
          font-weight: 600;
        }
        
        .tab-label {
          display: flex;
          align-items: center;
          gap: 8px;
          
          .el-icon {
            font-size: 18px;
          }
        }
      }
      
      .el-tabs__active-bar {
        height: 3px;
        background: linear-gradient(90deg, #409EFF 0%, #667eea 100%);
        border-radius: 3px 3px 0 0;
      }
    }
  }
  
  .tab-content {
    animation: fadeIn 0.4s ease-in-out;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .file-overview {
    grid-template-columns: 1fr !important;
  }
  
  .tabs-container {
    :deep(.result-tabs) {
      .el-tabs__item {
        padding: 0 20px;
        font-size: 14px;
        height: 50px;
        line-height: 50px;
      }
    }
  }
}
</style>

