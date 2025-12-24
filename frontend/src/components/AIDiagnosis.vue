<template>
  <div class="ai-diagnosis-card slide-in-up">
    <div class="card-title">
      <el-icon><MagicStick /></el-icon>
      AI智能诊断
      <span class="subtitle">（基于大模型的深度分析）</span>
    </div>
    
    <!-- 后端配置状态提示 -->
    <el-alert
      v-if="backendConfig.hasApiKey && backendConfig.hasDefaultModel"
      title="后端配置已就绪"
      type="success"
      :closable="false"
      show-icon
      style="margin-bottom: 20px"
    >
      <template #default>
        <p>✅ API已配置 | 🤖 模型: {{ backendConfig.defaultModel }}</p>
        <p style="margin-top: 5px; font-size: 12px; color: #67C23A;">可直接点击"开始AI诊断"，无需填写配置</p>
      </template>
    </el-alert>
    
    <!-- 配置区域 -->
    <div class="config-section" v-if="!diagnosing && !diagnosis">
      <el-form :model="config" label-width="120px" size="large">
        <!-- API地址 - 仅在后端未配置时显示 -->
        <el-form-item label="API地址" v-if="shouldShowApiUrl">
          <el-input 
            v-model="config.apiUrl" 
            placeholder="留空使用后端配置的默认地址（如已配置OpenRouter）"
            clearable
          >
            <template #prepend>
              <el-icon><Link /></el-icon>
            </template>
          </el-input>
          <div class="form-tip">如已在后端配置，留空即可；否则请填写完整API地址</div>
        </el-form-item>
        
        <!-- API Key - 仅在后端未配置时显示 -->
        <el-form-item label="API Key" v-if="shouldShowApiKey">
          <el-input 
            v-model="config.apiKey" 
            placeholder="请输入API Key"
            type="password"
            show-password
            clearable
          >
            <template #prepend>
              <el-icon><Key /></el-icon>
            </template>
          </el-input>
          <div class="form-tip">建议在后端配置文件中设置，无需每次输入</div>
        </el-form-item>
        
        <!-- 模型选择 - 仅在后端未配置时显示 -->
        <el-form-item label="模型" v-if="shouldShowModelSelect">
          <el-select v-model="config.model" placeholder="选择模型" clearable>
            <el-option label="OpenAI GPT-4o" value="gpt-4o" />
            <el-option label="OpenAI GPT-4" value="gpt-4" />
            <el-option label="OpenAI GPT-4 Turbo" value="gpt-4-turbo-preview" />
            <el-option label="OpenAI GPT-3.5 Turbo" value="gpt-3.5-turbo" />
            <el-option label="OpenRouter GPT-4o" value="openai/gpt-4o" />
            <el-option label="OpenRouter Claude 3.5 Sonnet" value="anthropic/claude-3.5-sonnet" />
            <el-option label="Google Gemini Flash" value="google/gemini-flash-1.5" />
          </el-select>
          <div class="form-tip" v-if="config.model && config.model.includes('/')">
            OpenRouter格式: provider/model-name
          </div>
        </el-form-item>
        
        <!-- 当前使用的模型显示 -->
        <el-form-item label="当前模型" v-else>
          <el-tag type="success" size="large">{{ currentModelDisplay }}</el-tag>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            size="large"
            :icon="MagicStick"
            @click="startDiagnosis"
            :disabled="!gcLogFile || (!backendConfig.hasApiKey && !config.apiKey)"
            style="width: 100%"
          >
            开始AI诊断
          </el-button>
        </el-form-item>
        
        <div class="tips-box">
          <el-icon><InfoFilled /></el-icon>
          <div>
            <p><strong>配置说明：</strong></p>
            <ul>
              <li><strong>推荐方式</strong>：在后端 application.yml 中配置 AI 相关参数，前端无需填写</li>
              <li>API Key 和 API 地址已在后端配置时，可直接点击"开始AI诊断"</li>
              <li>支持 OpenAI 官方 API 和兼容服务（如 <a href="https://openrouter.ai" target="_blank">OpenRouter</a>）</li>
              <li>OpenRouter 模型格式：<code>provider/model-name</code>（如 <code>openai/gpt-4o</code>）</li>
              <li>首次诊断可能需要20-60秒，请耐心等待</li>
            </ul>
          </div>
        </div>
      </el-form>
    </div>
    
    <!-- 诊断进行中 -->
    <div class="diagnosing-section" v-if="diagnosing">
      <div class="loading-animation">
        <el-icon class="rotating"><Loading /></el-icon>
      </div>
      <h3>AI正在深度分析您的GC日志...</h3>
      <p class="loading-tips">{{ loadingTip }}</p>
      <el-progress 
        :percentage="progress" 
        :stroke-width="10"
        :show-text="false"
        :indeterminate="true"
        status="success"
      />
      <p class="time-elapsed">已耗时: {{ elapsedTime }}s</p>
    </div>
    
    <!-- 诊断结果 -->
    <div class="diagnosis-result" v-if="diagnosis && !diagnosing">
      <div class="result-header">
        <el-tag type="success" size="large">
          <el-icon><Check /></el-icon>
          诊断完成
        </el-tag>
        <span class="process-time">耗时: {{ processTime }}s</span>
        <el-button 
          type="primary" 
          :icon="Refresh"
          @click="resetDiagnosis"
          size="small"
        >
          重新诊断
        </el-button>
      </div>
      
      <!-- Markdown渲染区域 -->
      <div class="markdown-content" v-html="renderedMarkdown"></div>
    </div>
    
    <!-- 错误提示 -->
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      style="margin-top: 20px"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { MagicStick, Link, Key, InfoFilled, Loading, Check, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAIDiagnosisConfig, performAIDiagnosis } from '../api/aiDiagnosis'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

const props = defineProps({
  gcLogFile: {
    type: File,
    default: null
  },
  collectorType: {
    type: String,
    default: 'Unknown'
  },
  eventCount: {
    type: Number,
    default: 0
  }
})

// 后端配置信息
const backendConfig = ref({
  hasApiKey: false,
  hasApiUrl: false,
  apiUrl: '',
  hasDefaultModel: false,
  defaultModel: ''
})

// 用户配置
const config = ref({
  apiUrl: '',
  apiKey: '',
  model: ''
})

// 状态
const diagnosing = ref(false)
const diagnosis = ref(null)
const error = ref('')
const elapsedTime = ref(0)
const progress = ref(0)
const processTime = ref(0)

let timer = null

// 加载提示
const loadingTips = [
  '正在发送GC日志到AI模型...',
  'AI正在分析GC行为模式...',
  '正在评估内存使用趋势...',
  '正在识别性能瓶颈...',
  '正在生成优化建议...'
]
const loadingTip = ref(loadingTips[0])

// Markdown渲染器
const md = new MarkdownIt({
  highlight: (str, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (__) {}
    }
    return ''
  },
  html: true,
  linkify: true,
  typographer: true
})

const renderedMarkdown = computed(() => {
  return diagnosis.value ? md.render(diagnosis.value) : ''
})

// 获取后端配置
onMounted(async () => {
  try {
    const response = await getAIDiagnosisConfig()
    backendConfig.value = response.data
    
    console.log('后端配置:', backendConfig.value)
    
    // 如果后端已配置，显示提示
    if (backendConfig.value.hasApiKey && backendConfig.value.hasDefaultModel) {
      ElMessage.success({
        message: '后端AI配置已就绪，可直接开始诊断',
        duration: 3000
      })
    }
  } catch (err) {
    console.error('获取后端配置失败:', err)
  }
})

// 显示或隐藏配置项
const shouldShowApiUrl = computed(() => !backendConfig.value.hasApiUrl)
const shouldShowApiKey = computed(() => !backendConfig.value.hasApiKey)
const shouldShowModelSelect = computed(() => !backendConfig.value.hasDefaultModel)

// 获取当前使用的模型名称（用于显示）
const currentModelDisplay = computed(() => {
  if (backendConfig.value.hasDefaultModel) {
    return backendConfig.value.defaultModel
  }
  return config.value.model || '未选择'
})

// 开始诊断
async function startDiagnosis() {
  if (!props.gcLogFile) {
    ElMessage.warning('请先上传GC日志文件')
    return
  }
  
  // 验证配置：如果后端没有配置API Key，前端必须提供
  if (!backendConfig.value.hasApiKey && !config.value.apiKey) {
    ElMessage.warning('请输入API Key或在后端配置文件中设置')
    return
  }

  diagnosing.value = true
  error.value = ''
  elapsedTime.value = 0
  progress.value = 0
  
  // 开始计时和进度
  timer = setInterval(() => {
    elapsedTime.value++
    const tipIndex = Math.min(Math.floor(elapsedTime.value / 10), loadingTips.length - 1)
    loadingTip.value = loadingTips[tipIndex]
  }, 1000)

  try {
    const response = await performAIDiagnosis(
      props.gcLogFile,
      config.value.apiUrl, // 可能为空，使用后端配置
      config.value.apiKey, // 可能为空，使用后端配置
      config.value.model, // 可能为空，使用后端配置
      props.collectorType,
      props.eventCount
    )

    if (response.data.success) {
      diagnosis.value = response.data.diagnosis
      processTime.value = (response.data.processTime / 1000).toFixed(1)
      ElMessage.success('AI诊断完成！')
    } else {
      throw new Error(response.data.error || 'AI诊断失败')
    }
  } catch (err) {
    console.error('AI诊断失败:', err)
    error.value = err.response?.data?.error || err.message || 'AI诊断失败，请检查配置和网络连接'
    ElMessage.error(error.value)
  } finally {
    diagnosing.value = false
    clearInterval(timer)
  }
}

// 重置诊断
function resetDiagnosis() {
  diagnosis.value = null
  error.value = ''
  elapsedTime.value = 0
  progress.value = 0
  processTime.value = 0
}

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style lang="scss" scoped>
.ai-diagnosis-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.3);
  color: white;
  margin-bottom: 30px;

  .card-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 24px;
    font-weight: 600;
    margin-bottom: 20px;

    .subtitle {
      font-size: 14px;
      opacity: 0.8;
      font-weight: 400;
    }
  }

  .config-section {
    background: rgba(255, 255, 255, 0.1);
    padding: 20px;
    border-radius: 8px;
    backdrop-filter: blur(10px);

    .form-tip {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.7);
      margin-top: 5px;
    }

    .tips-box {
      display: flex;
      gap: 10px;
      background: rgba(255, 255, 255, 0.1);
      padding: 15px;
      border-radius: 8px;
      margin-top: 20px;

      ul {
        margin: 10px 0 0 0;
        padding-left: 20px;

        li {
          margin-bottom: 5px;
          font-size: 13px;
        }
      }

      a {
        color: #90caf9;
        text-decoration: underline;
      }

      code {
        background: rgba(0, 0, 0, 0.2);
        padding: 2px 6px;
        border-radius: 3px;
        font-family: 'Courier New', monospace;
      }
    }
  }

  .diagnosing-section {
    text-align: center;
    padding: 40px 20px;

    .loading-animation {
      margin-bottom: 20px;

      .rotating {
        font-size: 60px;
        animation: rotate 2s linear infinite;
      }
    }

    h3 {
      font-size: 20px;
      margin-bottom: 10px;
    }

    .loading-tips {
      font-size: 14px;
      opacity: 0.8;
      margin-bottom: 20px;
    }

    .time-elapsed {
      margin-top: 15px;
      font-size: 14px;
      opacity: 0.7;
    }
  }

  .diagnosis-result {
    .result-header {
      display: flex;
      align-items: center;
      gap: 15px;
      margin-bottom: 20px;
      padding-bottom: 15px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.2);

      .process-time {
        opacity: 0.8;
      }
    }

    .markdown-content {
      background: white;
      color: #333;
      padding: 25px;
      border-radius: 8px;
      max-height: 600px;
      overflow-y: auto;

      :deep(h1), :deep(h2), :deep(h3) {
        margin-top: 20px;
        margin-bottom: 10px;
        color: #333;
      }

      :deep(h2) {
        border-bottom: 2px solid #eee;
        padding-bottom: 10px;
      }

      :deep(ul), :deep(ol) {
        margin: 10px 0;
        padding-left: 30px;

        li {
          margin: 5px 0;
        }
      }

      :deep(pre) {
        background: #2d2d2d;
        color: #f8f8f2;
        padding: 15px;
        border-radius: 5px;
        overflow-x: auto;
        margin: 15px 0;

        code {
          background: none;
          padding: 0;
          font-family: 'Fira Code', 'Courier New', monospace;
        }
      }

      :deep(code) {
        background: #f5f5f5;
        padding: 2px 6px;
        border-radius: 3px;
        font-family: 'Courier New', monospace;
        color: #e83e8c;
      }

      :deep(blockquote) {
        border-left: 4px solid #667eea;
        padding-left: 15px;
        margin: 15px 0;
        color: #666;
      }

      :deep(table) {
        width: 100%;
        border-collapse: collapse;
        margin: 15px 0;

        th, td {
          border: 1px solid #ddd;
          padding: 10px;
          text-align: left;
        }

        th {
          background: #f5f5f5;
          font-weight: 600;
        }
      }
    }
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.slide-in-up {
  animation: slideInUp 0.5s ease-out;
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
