import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 300000, // 5分钟超时
  headers: {
    'Content-Type': 'multipart/form-data'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

/**
 * 上传并分析GC日志
 */
export function analyzeGCLog(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  
  return api.post('/gc/analyze', formData, {
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percentCompleted)
      }
    }
  })
}

/**
 * 健康检查
 */
export function healthCheck() {
  return api.get('/gc/health')
}

/**
 * 获取支持的GC收集器
 */
export function getSupportedCollectors() {
  return api.get('/gc/collectors')
}

/**
 * 导出分析结果为HTML（保持原有页面样式）
 */
export function exportAnalysisToHtml(renderedHtml, analysisData) {
  const formData = new FormData()
  formData.append('renderedHtml', renderedHtml)
  formData.append('analysisData', JSON.stringify(analysisData))
  
  return axios.post('/api/gc/export/html', formData, {
    responseType: 'blob',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 30000
  })
}

export default api

