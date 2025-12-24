import axios from 'axios'

// 创建专用的AI诊断API实例
const aiApi = axios.create({
  baseURL: '/api',
  timeout: 90000, // 90秒超时
  headers: {
    'Content-Type': 'multipart/form-data'
  }
})

/**
 * 获取后端AI诊断配置
 */
export function getAIDiagnosisConfig() {
  return aiApi.get('/ai/config')
}

/**
 * 执行AI诊断
 */
export function performAIDiagnosis(file, apiUrl, apiKey, model, collectorType, eventCount) {
  const formData = new FormData()
  formData.append('file', file)
  
  // 只有当用户填写了才发送，否则使用后端配置
  if (apiKey) {
    formData.append('apiKey', apiKey)
  }
  if (apiUrl) {
    formData.append('apiUrl', apiUrl)
  }
  if (model) {
    formData.append('model', model)
  }
  if (collectorType) {
    formData.append('collectorType', collectorType)
  }
  formData.append('eventCount', eventCount)

  return aiApi.post('/ai/diagnose', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 90000 // 90秒超时
  })
}


