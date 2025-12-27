<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><Warning /></el-icon>
      诊断报告与优化建议
    </div>
    
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
      
      <!-- AI智能优化建议 - 整合在优化建议模块内 -->
      <div class="ai-optimization-wrapper">
        <el-divider content-position="left">
          <el-icon><MagicStick /></el-icon>
          <span style="margin-left: 6px">AI智能优化建议</span>
        </el-divider>
        
        <!-- AI按钮 - 精简版 -->
        <div class="ai-button-wrapper">
          <el-button 
            type="primary" 
            :icon="MagicStick" 
            @click="getAIOptimization"
            :loading="aiLoading"
            :disabled="!backendConfig.hasApiKey"
            plain
          >
            {{ aiLoading ? '正在生成...' : (aiDiagnosis ? '重新生成' : '获取AI优化建议') }}
          </el-button>
          <span class="ai-hint" v-if="!backendConfig.hasApiKey">
            需先配置API Key
          </span>
        </div>
        
        <!-- AI诊断错误 -->
        <el-alert
          v-if="aiError"
          :title="aiError"
          type="error"
          show-icon
          closable
          @close="aiError = null"
          style="margin-top: 16px"
        />
        
        <!-- AI优化建议结果 - 可折叠 -->
        <el-collapse v-if="aiDiagnosis" v-model="activeCollapse" style="margin-top: 16px">
          <el-collapse-item name="ai-result">
            <template #title>
              <div class="collapse-title">
                <el-icon><Document /></el-icon>
                <span>AI诊断报告</span>
                <el-tag type="success" size="small" style="margin-left: 12px">
                  已生成
                </el-tag>
                <el-button 
                  :icon="Download" 
                  @click.stop="exportAIDiagnosis"
                  size="small"
                  text
                  style="margin-left: auto"
                >
                  导出
                </el-button>
              </div>
            </template>
            
            <!-- AI诊断内容渲染 -->
            <div class="markdown-content" v-html="renderedMarkdown"></div>
            
            <!-- 处理时间 -->
            <div class="ai-meta">
              <el-tag type="info" size="small">
                处理时间: {{ aiProcessTime }}ms
              </el-tag>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Warning, Search, CircleClose, Clock, Tickets, MagicStick, Download, Document } from '@element-plus/icons-vue'
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
const activeCollapse = ref(['ai-result']) // 默认展开

// 后端配置信息（与AI诊断模块共享）
const backendConfig = ref({
  hasApiKey: false,
  hasApiUrl: false,
  hasDefaultModel: false,
  defaultModel: ''
})

// 生成缓存键
const getCacheKey = () => {
  if (!props.analysisResult) return null
  // 使用文件名和GC事件数作为缓存键
  const fileName = props.analysisResult.fileName || 'unknown'
  const eventCount = props.analysisResult.gcEvents?.length || 0
  const collectorType = props.analysisResult.collectorType || 'unknown'
  return `ai_diagnosis_${fileName}_${collectorType}_${eventCount}`
}

// 从缓存加载
const loadFromCache = () => {
  const cacheKey = getCacheKey()
  if (!cacheKey) return false
  
  try {
    const cached = localStorage.getItem(cacheKey)
    if (cached) {
      const data = JSON.parse(cached)
      // 检查缓存是否过期（24小时）
      const cacheTime = data.timestamp || 0
      const now = Date.now()
      if (now - cacheTime < 24 * 60 * 60 * 1000) {
        aiDiagnosis.value = data.diagnosis
        aiProcessTime.value = data.processTime || 0
        console.log('已从缓存加载AI诊断结果')
        return true
      } else {
        // 缓存过期，删除
        localStorage.removeItem(cacheKey)
      }
    }
  } catch (err) {
    console.error('加载缓存失败:', err)
  }
  return false
}

// 保存到缓存
const saveToCache = (diagnosis, processTime) => {
  const cacheKey = getCacheKey()
  if (!cacheKey) return
  
  try {
    const data = {
      diagnosis,
      processTime,
      timestamp: Date.now()
    }
    localStorage.setItem(cacheKey, JSON.stringify(data))
    console.log('AI诊断结果已缓存')
  } catch (err) {
    console.error('保存缓存失败:', err)
  }
}

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
    
    // 尝试从缓存加载
    loadFromCache()
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
      
      // 保存到缓存
      saveToCache(response.diagnosis, response.processTime)
      
      // 自动展开结果
      activeCollapse.value = ['ai-result']
      
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
// AI优化建议包装器
.ai-optimization-wrapper {
  margin-top: 24px;
  padding-top: 16px;
}

// AI按钮区域 - 精简样式
.ai-button-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .el-button {
    min-width: 140px;
  }
  
  .ai-hint {
    font-size: 13px;
    color: #909399;
  }
}

// 折叠面板标题
.collapse-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
  width: 100%;
  
  .el-icon {
    color: #667eea;
  }
}

// Markdown内容样式
.markdown-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  line-height: 1.8;
  margin-top: 12px;
  
  :deep(h1) {
    font-size: 24px;
    font-weight: 700;
    color: #1a1a1a;
    margin-top: 0;
    margin-bottom: 16px;
    padding-bottom: 10px;
    border-bottom: 2px solid #667eea;
  }
  
  :deep(h2) {
    color: #303133;
    font-size: 20px;
    font-weight: 600;
    margin-top: 24px;
    margin-bottom: 14px;
    padding-left: 10px;
    border-left: 4px solid #667eea;
  }
  
  :deep(h3) {
    color: #606266;
    font-size: 16px;
    font-weight: 600;
    margin-top: 20px;
    margin-bottom: 12px;
    
    &::before {
      content: '▸';
      color: #667eea;
      margin-right: 6px;
    }
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
    font-size: 14px;
  }
  
  :deep(ul), :deep(ol) {
    padding-left: 24px;
    margin-bottom: 12px;
    
    li {
      margin-bottom: 8px;
      color: #606266;
      line-height: 1.6;
      
      &::marker {
        color: #667eea;
      }
    }
  }
  
  :deep(code) {
    background: #f5f7fa;
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 13px;
    color: #e83e8c;
    border: 1px solid #e1e4e8;
  }
  
  :deep(pre) {
    background: #1e1e1e;
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 16px 0;
    border: 1px solid #333;
    
    code {
      background: transparent;
      color: #d4d4d4;
      padding: 0;
      border: none;
      font-size: 13px;
    }
  }
  
  :deep(blockquote) {
    border-left: 4px solid #667eea;
    padding: 12px 16px;
    margin: 16px 0;
    background: linear-gradient(135deg, #f0f4ff 0%, #f5f0ff 100%);
    border-radius: 0 8px 8px 0;
    color: #5a6c7d;
    
    p {
      margin: 0;
    }
  }
  
  :deep(table) {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
    margin: 16px 0;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

    th, td {
      border: 1px solid #e1e4e8;
      padding: 10px 14px;
      text-align: left;
    }

    th {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      font-weight: 600;
      font-size: 13px;
    }

    tbody tr {
      background: white;
      
      &:nth-child(even) {
        background: #f8f9fa;
      }
      
      &:hover {
        background: #e3f2fd;
      }
    }

    td {
      color: #4a5568;
      font-size: 13px;
    }
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

