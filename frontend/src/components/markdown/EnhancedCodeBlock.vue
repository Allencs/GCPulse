<template>
  <div class="enhanced-code-block">
    <!-- 工具栏 -->
    <div class="code-toolbar">
      <div class="toolbar-left">
        <span class="lang-badge">
          <el-icon><Document /></el-icon>
          {{ displayLang }}
        </span>
        <span class="line-count">{{ lineCount }} 行</span>
      </div>
      
      <div class="toolbar-right">
        <el-tooltip content="自动换行" placement="top">
          <button 
            @click="toggleWrap" 
            :class="['icon-btn', { active: isWrap }]"
          >
            <el-icon><Fold /></el-icon>
          </button>
        </el-tooltip>
        
        <el-tooltip :content="copied ? '已复制!' : '复制代码'" placement="top">
          <button 
            @click="copyCode" 
            :class="['icon-btn', { copied }]"
          >
            <el-icon v-if="!copied"><CopyDocument /></el-icon>
            <el-icon v-else><Check /></el-icon>
          </button>
        </el-tooltip>
        
        <el-tooltip content="下载代码" placement="top">
          <button @click="downloadCode" class="icon-btn">
            <el-icon><Download /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>
    
    <!-- 代码内容 -->
    <div class="code-content-wrapper">
      <!-- 行号 -->
      <div class="line-numbers" v-if="showLineNumbers">
        <span 
          v-for="n in lineCount" 
          :key="n"
          :class="{ highlight: highlightedLines.includes(n) }"
        >
          {{ n }}
        </span>
      </div>
      
      <!-- 代码 -->
      <pre :class="{ wrap: isWrap }"><code v-html="highlightedCode"></code></pre>
    </div>
    
    <!-- 复制成功提示 -->
    <transition name="fade">
      <div v-if="showCopiedToast" class="copied-toast">
        ✅ 代码已复制到剪贴板
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Document, CopyDocument, Download, Fold, Check } from '@element-plus/icons-vue'
import hljs from 'highlight.js'

const props = defineProps({
  code: {
    type: String,
    required: true
  },
  language: {
    type: String,
    default: 'text'
  },
  showLineNumbers: {
    type: Boolean,
    default: true
  },
  highlightedLines: {
    type: Array,
    default: () => []
  }
})

// 状态
const copied = ref(false)
const isWrap = ref(false)
const showCopiedToast = ref(false)

// 计算属性
const displayLang = computed(() => {
  const langMap = {
    js: 'JavaScript',
    ts: 'TypeScript',
    java: 'Java',
    python: 'Python',
    bash: 'Bash',
    sql: 'SQL',
    yaml: 'YAML',
    json: 'JSON',
    xml: 'XML',
    html: 'HTML',
    css: 'CSS',
    scss: 'SCSS'
  }
  return langMap[props.language] || props.language.toUpperCase()
})

const lineCount = computed(() => {
  return props.code.split('\n').length
})

const highlightedCode = computed(() => {
  try {
    if (props.language && hljs.getLanguage(props.language)) {
      return hljs.highlight(props.code, { language: props.language }).value
    }
  } catch (e) {
    console.warn('代码高亮失败:', e)
  }
  return props.code
})

// 方法
async function copyCode() {
  try {
    await navigator.clipboard.writeText(props.code)
    copied.value = true
    showCopiedToast.value = true
    
    setTimeout(() => {
      copied.value = false
      showCopiedToast.value = false
    }, 2000)
  } catch (err) {
    console.error('复制失败:', err)
  }
}

function toggleWrap() {
  isWrap.value = !isWrap.value
}

function downloadCode() {
  const blob = new Blob([props.code], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `code.${props.language}`
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<style lang="scss" scoped>
.enhanced-code-block {
  position: relative;
  margin: 20px 0;
  border-radius: 12px;
  overflow: hidden;
  background: #1e1e1e;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  border: 1px solid #333;
  
  // 工具栏
  .code-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: linear-gradient(135deg, #2d2d2d 0%, #1e1e1e 100%);
    border-bottom: 1px solid #333;
    
    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .lang-badge {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 4px 12px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border-radius: 6px;
        font-size: 12px;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }
      
      .line-count {
        font-size: 12px;
        color: #888;
        font-family: 'Monaco', monospace;
      }
    }
    
    .toolbar-right {
      display: flex;
      gap: 8px;
      
      .icon-btn {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 6px 10px;
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 6px;
        color: #aaa;
        cursor: pointer;
        transition: all 0.2s;
        font-size: 12px;
        
        &:hover {
          background: rgba(255, 255, 255, 0.1);
          color: #fff;
          border-color: rgba(255, 255, 255, 0.2);
          transform: translateY(-1px);
        }
        
        &.active {
          background: rgba(102, 126, 234, 0.2);
          border-color: #667eea;
          color: #667eea;
        }
        
        &.copied {
          background: rgba(103, 194, 58, 0.2);
          border-color: #67c23a;
          color: #67c23a;
        }
      }
    }
  }
  
  // 代码内容
  .code-content-wrapper {
    display: flex;
    position: relative;
    
    .line-numbers {
      display: flex;
      flex-direction: column;
      padding: 20px 0;
      background: #252525;
      border-right: 1px solid #333;
      user-select: none;
      
      span {
        padding: 0 16px;
        text-align: right;
        font-size: 13px;
        line-height: 1.6;
        color: #666;
        font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
        
        &.highlight {
          background: rgba(102, 126, 234, 0.2);
          color: #667eea;
          font-weight: 600;
        }
      }
    }
    
    pre {
      flex: 1;
      margin: 0;
      padding: 20px;
      overflow-x: auto;
      background: transparent;
      
      &.wrap {
        white-space: pre-wrap;
        word-break: break-word;
      }
      
      code {
        display: block;
        font-family: 'Monaco', 'Menlo', 'Consolas', 'Courier New', monospace;
        font-size: 13px;
        line-height: 1.6;
        color: #d4d4d4;
      }
    }
  }
  
  // 复制提示
  .copied-toast {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    padding: 12px 24px;
    background: rgba(103, 194, 58, 0.95);
    color: white;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 600;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
    z-index: 10;
  }
}

// 滚动条美化
.code-content-wrapper pre {
  &::-webkit-scrollbar {
    height: 8px;
  }
  
  &::-webkit-scrollbar-track {
    background: #2d2d2d;
  }
  
  &::-webkit-scrollbar-thumb {
    background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
    border-radius: 4px;
    
    &:hover {
      background: linear-gradient(90deg, #5568d3 0%, #653a8b 100%);
    }
  }
}

// 动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

