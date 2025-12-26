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
 * 执行AI诊断（基于原始日志）
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

/**
 * 获取AI智能优化建议（基于结构化分析结果） - 新增
 */
export async function getAIOptimizationSuggestions(analysisResult, apiUrl, apiKey, model) {
  try {
    const params = {}
    
    // 只有当用户填写了才发送，否则使用后端配置
    if (apiKey) {
      params.apiKey = apiKey
    }
    if (apiUrl) {
      params.apiUrl = apiUrl
    }
    if (model) {
      params.model = model
    }
    
    const response = await aiApi.post('/ai/optimize', JSON.stringify(analysisResult), {
      params: params,
      headers: {
        'Content-Type': 'application/json'
      },
      timeout: 120000 // 120秒超时（结构化数据处理可能需要更多时间）
    })
    
    return response.data
  } catch (error) {
    console.error('AI优化建议请求失败:', error)
    throw error
  }
}

/**
 * 导出诊断报告为HTML（使用渲染后的HTML）
 */
export function exportToHtml(renderedHtml, diagnosis, collectorType, eventCount) {
  const formData = new FormData()
  formData.append('renderedHtml', renderedHtml)
  formData.append('diagnosis', diagnosis)
  if (collectorType) {
    formData.append('collectorType', collectorType)
  }
  if (eventCount) {
    formData.append('eventCount', eventCount)
  }

  return aiApi.post('/ai/export/html', formData, {
    responseType: 'blob',
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 导出诊断报告为Markdown
 */
export function exportToMarkdown(diagnosis, collectorType, eventCount) {
  const formData = new FormData()
  formData.append('diagnosis', diagnosis)
  if (collectorType) {
    formData.append('collectorType', collectorType)
  }
  if (eventCount) {
    formData.append('eventCount', eventCount)
  }

  return aiApi.post('/ai/export/markdown', formData, {
    responseType: 'blob',
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
