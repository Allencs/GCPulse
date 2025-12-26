<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><Warning /></el-icon>
      诊断报告与优化建议
    </div>
    
    <!-- AI智能优化建议按钮 -->
    <div class="ai-optimization-section">
      <!-- 配置状态提示 -->
      <el-alert
        v-if="!backendConfig.hasApiKey"
        title="需要配置AI服务"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #default>
          <p>请先在"<strong>AI智能诊断</strong>"标签页配置API Key，或在后端配置文件中设置</p>
          <p style="margin-top: 5px; font-size: 12px; color: #E6A23C;">配置完成后即可使用AI智能优化建议功能</p>
        </template>
      </el-alert>
      
      <el-alert
        v-else
        title="AI配置已就绪"
        type="success"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #default>
          <p>✅ 已使用后端配置 | 🤖 模型: {{ backendConfig.defaultModel || '默认' }}</p>
          <p style="margin-top: 5px; font-size: 12px; color: #67C23A;">可直接点击下方按钮获取AI优化建议</p>
        </template>
      </el-alert>
      
      <el-button 
        type="primary" 
        :icon="MagicStick" 
        @click="getAIOptimization"
        :loading="aiLoading"
        :disabled="!backendConfig.hasApiKey"
        size="large"
      >
        <span v-if="!aiDiagnosis">{{ aiLoading ? '正在生成AI优化建议...' : '🤖 获取AI智能优化建议' }}</span>
        <span v-else>🔄 重新生成AI建议</span>
      </el-button>
      <p class="ai-description">
        基于GC分析结果，使用AI深度分析并提供专业的JVM调优建议
      </p>
    </div>
    
    <!-- AI优化建议结果 -->
    <div v-if="aiDiagnosis" class="ai-diagnosis-result">
      <div class="ai-result-header">
        <h3>
          <el-icon><MagicStick /></el-icon>
          AI智能优化建议
        </h3>
        <el-button 
          :icon="Download" 
          @click="exportAIDiagnosis"
          size="small"
        >
          导出报告
        </el-button>
      </div>
      
      <!-- AI诊断内容渲染 -->
      <div class="markdown-content" v-html="renderedMarkdown"></div>
      
      <!-- 处理时间 -->
      <div class="ai-meta">
        <el-tag type="info" size="small">
          处理时间: {{ aiProcessTime }}ms
        </el-tag>
      </div>
    </div>
    
    <!-- AI诊断错误 -->
    <el-alert
      v-if="aiError"
      :title="aiError"
      type="error"
      show-icon
      closable
      @close="aiError = null"
    />
    
    <el-divider />
    
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
import { ref, computed, onMounted } from 'vue'
import { Warning, Search, CircleClose, Clock, Tickets, MagicStick, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { getAIOptimizationSuggestions, getAIDiagnosisConfig } from '../api/aiDiagnosis'

const props = defineProps({
  diagnosisReport: {
    type: Object,
    default: () => ({})
  },
  analysisResult: {
    type: Object,
    default: () => null
  }
})

const aiDiagnosis = ref(null)
const aiLoading = ref(false)
const aiError = ref(null)
const aiProcessTime = ref(0)

// 后端配置信息（与AI诊断模块共享）
const backendConfig = ref({
  hasApiKey: false,
  hasApiUrl: false,
  hasDefaultModel: false,
  defaultModel: ''
})

// 配置Markdown解析器
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (__) {}
    }
    return ''
  }
})

// 渲染Markdown
const renderedMarkdown = computed(() => {
  if (!aiDiagnosis.value) return ''
  return md.render(aiDiagnosis.value)
})

// 获取后端配置（复用AI诊断模块的配置）
onMounted(async () => {
  try {
    const response = await getAIDiagnosisConfig()
    backendConfig.value = response.data
    console.log('后端AI配置:', backendConfig.value)
  } catch (err) {
    console.error('获取后端配置失败:', err)
  }
})

// 获取AI优化建议
async function getAIOptimization() {
  if (!props.analysisResult) {
    ElMessage.warning('没有可用的分析结果')
    return
  }
  
  // 检查API Key配置：优先使用后端配置，否则提示用户需要配置
  if (!backendConfig.value.hasApiKey) {
    ElMessage.warning({
      message: '请先在"AI智能诊断"标签页配置API Key，或在后端配置文件中设置',
      duration: 5000,
      showClose: true
    })
    return
  }
  
  aiLoading.value = true
  aiError.value = null
  
  try {
    // 使用空字符串，让后端使用配置的值
    const response = await getAIOptimizationSuggestions(
      props.analysisResult,
      '', // apiUrl - 使用后端配置
      '', // apiKey - 使用后端配置
      ''  // model - 使用后端配置
    )
    
    if (response.success) {
      aiDiagnosis.value = response.diagnosis
      aiProcessTime.value = response.processTime
      ElMessage.success('AI优化建议生成成功')
    } else {
      aiError.value = response.error || '生成失败'
      ElMessage.error('生成AI优化建议失败: ' + aiError.value)
    }
  } catch (error) {
    console.error('AI优化建议失败:', error)
    aiError.value = error.message || '未知错误'
    ElMessage.error('生成AI优化建议失败: ' + aiError.value)
  } finally {
    aiLoading.value = false
  }
}

// 导出AI诊断报告
function exportAIDiagnosis() {
  if (!aiDiagnosis.value) return
  
  // 创建Markdown文件并下载
  const blob = new Blob([aiDiagnosis.value], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `GCPulse_AI_Optimization_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.md`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  
  ElMessage.success('AI优化报告已导出')
}

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
.ai-optimization-section {
  margin-bottom: 32px;
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  text-align: center;
  
  .el-button {
    font-size: 16px;
    padding: 16px 32px;
    border: none;
    background: white;
    color: #667eea;
    font-weight: 600;
    
    &:hover {
      background: #f5f7fa;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
  }
  
  .ai-description {
    margin-top: 12px;
    color: white;
    font-size: 14px;
    opacity: 0.9;
  }
}

.ai-diagnosis-result {
  margin-bottom: 32px;
  padding: 24px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 2px solid #667eea;
  
  .ai-result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h3 {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      font-weight: 600;
      color: #667eea;
      margin: 0;
      
      .el-icon {
        font-size: 20px;
      }
    }
  }
  
  .markdown-content {
    background: white;
    padding: 20px;
    border-radius: 8px;
    line-height: 1.8;
    
    :deep(h2) {
      color: #303133;
      font-size: 20px;
      font-weight: 600;
      margin-top: 24px;
      margin-bottom: 16px;
      padding-bottom: 8px;
      border-bottom: 2px solid #e4e7ed;
    }
    
    :deep(h3) {
      color: #606266;
      font-size: 16px;
      font-weight: 600;
      margin-top: 20px;
      margin-bottom: 12px;
    }
    
    :deep(h4) {
      color: #606266;
      font-size: 15px;
      font-weight: 600;
      margin-top: 16px;
      margin-bottom: 10px;
    }
    
    :deep(p) {
      margin-bottom: 12px;
      color: #606266;
    }
    
    :deep(ul), :deep(ol) {
      padding-left: 24px;
      margin-bottom: 12px;
      
      li {
        margin-bottom: 8px;
        color: #606266;
      }
    }
    
    :deep(code) {
      background: #f5f7fa;
      padding: 2px 6px;
      border-radius: 4px;
      font-family: 'Monaco', 'Menlo', monospace;
      font-size: 13px;
      color: #e83e8c;
    }
    
    :deep(pre) {
      background: #282c34;
      padding: 16px;
      border-radius: 8px;
      overflow-x: auto;
      margin: 16px 0;
      
      code {
        background: transparent;
        color: #abb2bf;
        padding: 0;
      }
    }
    
    :deep(blockquote) {
      border-left: 4px solid #409eff;
      padding-left: 16px;
      margin: 16px 0;
      color: #606266;
      background: #ecf5ff;
      padding: 12px 16px;
      border-radius: 4px;
    }
    
    :deep(strong) {
      color: #303133;
      font-weight: 600;
    }
  }
  
  .ai-meta {
    margin-top: 16px;
    text-align: right;
  }
}

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

