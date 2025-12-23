<template>
  <div class="file-upload-component">
    <div class="card-title">
      <el-icon><Upload /></el-icon>
      上传GC日志文件
    </div>
    
    <el-upload
      ref="uploadRef"
      class="upload-demo"
      drag
      :auto-upload="false"
      :limit="1"
      :on-change="handleFileChange"
      :on-exceed="handleExceed"
      accept=".log,.txt"
    >
      <div class="upload-content">
        <el-icon class="upload-icon" :size="64" color="#409EFF">
          <UploadFilled />
        </el-icon>
        <div class="upload-text">
          <p class="primary-text">点击或拖拽文件到此处上传</p>
          <p class="secondary-text">支持 .log 和 .txt 格式，最大500MB</p>
        </div>
      </div>
    </el-upload>
    
    <div v-if="selectedFile" class="file-info">
      <div class="file-details">
        <el-icon><Document /></el-icon>
        <span class="file-name">{{ selectedFile.name }}</span>
        <span class="file-size">{{ formatFileSize(selectedFile.size) }}</span>
      </div>
      <el-button 
        type="danger" 
        size="small" 
        :icon="Delete" 
        circle 
        @click="clearFile"
      />
    </div>
    
    <el-button
      v-if="selectedFile && !isAnalyzing"
      type="primary"
      size="large"
      class="analyze-btn"
      @click="startAnalysis"
      :loading="isAnalyzing"
    >
      <el-icon><DataAnalysis /></el-icon>
      开始分析
    </el-button>
    
    <div v-if="isAnalyzing" class="analyzing-section">
      <el-progress 
        :percentage="uploadProgress" 
        :status="uploadProgress === 100 ? 'success' : undefined"
      />
      <p class="analyzing-text">
        {{ uploadProgress < 100 ? '正在上传文件...' : '正在分析GC日志，请稍候...' }}
      </p>
    </div>
    
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
      class="error-alert"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, UploadFilled, Document, Delete, DataAnalysis } from '@element-plus/icons-vue'
import { analyzeGCLog } from '../api/gcAnalysis'

const emit = defineEmits(['analysis-complete'])

const uploadRef = ref()
const selectedFile = ref(null)
const isAnalyzing = ref(false)
const uploadProgress = ref(0)
const errorMessage = ref('')

function handleFileChange(file) {
  errorMessage.value = ''
  
  // 验证文件大小
  if (file.size > 500 * 1024 * 1024) {
    errorMessage.value = '文件大小超过限制（最大500MB）'
    return
  }
  
  // 验证文件类型
  const fileName = file.name.toLowerCase()
  if (!fileName.endsWith('.log') && !fileName.endsWith('.txt')) {
    errorMessage.value = '请上传 .log 或 .txt 格式的GC日志文件'
    return
  }
  
  selectedFile.value = file.raw
  ElMessage.success('文件选择成功')
}

function handleExceed() {
  ElMessage.warning('一次只能上传一个文件')
}

function clearFile() {
  selectedFile.value = null
  uploadProgress.value = 0
  errorMessage.value = ''
  uploadRef.value.clearFiles()
}

async function startAnalysis() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  isAnalyzing.value = true
  uploadProgress.value = 0
  errorMessage.value = ''
  
  try {
    const response = await analyzeGCLog(selectedFile.value, (progress) => {
      uploadProgress.value = progress
    })
    
    if (response.success && response.data) {
      ElMessage.success('分析完成！')
      emit('analysis-complete', response.data)
    } else {
      throw new Error(response.error || '分析失败')
    }
  } catch (error) {
    console.error('Analysis failed:', error)
    errorMessage.value = error.response?.data?.error || error.message || '分析失败，请重试'
    ElMessage.error(errorMessage.value)
  } finally {
    isAnalyzing.value = false
  }
}

function formatFileSize(bytes) {
  if (bytes >= 1024 * 1024 * 1024) {
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
  } else if (bytes >= 1024 * 1024) {
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  } else if (bytes >= 1024) {
    return (bytes / 1024).toFixed(2) + ' KB'
  }
  return bytes + ' B'
}
</script>

<style lang="scss" scoped>
.file-upload-component {
  .upload-demo {
    width: 100%;
    
    :deep(.el-upload) {
      width: 100%;
      
      .el-upload-dragger {
        width: 100%;
        padding: 40px;
        border: 2px dashed #d9d9d9;
        border-radius: 8px;
        transition: all 0.3s ease;
        
        &:hover {
          border-color: #409EFF;
          background: #f5f7fa;
        }
      }
    }
  }
  
  .upload-content {
    text-align: center;
    
    .upload-icon {
      margin-bottom: 16px;
    }
    
    .upload-text {
      .primary-text {
        font-size: 16px;
        color: #303133;
        font-weight: 500;
        margin-bottom: 8px;
      }
      
      .secondary-text {
        font-size: 14px;
        color: #909399;
      }
    }
  }
  
  .file-info {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;
    margin-top: 16px;
    
    .file-details {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
      
      .el-icon {
        font-size: 24px;
        color: #409EFF;
      }
      
      .file-name {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      .file-size {
        font-size: 13px;
        color: #909399;
      }
    }
  }
  
  .analyze-btn {
    width: 100%;
    margin-top: 20px;
    height: 48px;
    font-size: 16px;
    font-weight: 500;
  }
  
  .analyzing-section {
    margin-top: 24px;
    
    .el-progress {
      margin-bottom: 12px;
    }
    
    .analyzing-text {
      text-align: center;
      font-size: 14px;
      color: #606266;
    }
  }
  
  .error-alert {
    margin-top: 16px;
  }
}
</style>

