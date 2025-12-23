<template>
  <div class="home-view">
    <div class="welcome-section slide-in-up">
      <h2>欢迎使用 GC 日志分析平台</h2>
      <p class="welcome-desc">
        上传Java GC日志文件，获取专业的性能分析报告和优化建议
      </p>
      
      <div class="features-grid">
        <div class="feature-item" v-for="feature in features" :key="feature.title">
          <el-icon :size="32" :color="feature.color">
            <component :is="feature.icon" />
          </el-icon>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.desc }}</p>
        </div>
      </div>
    </div>
    
    <div class="upload-section analysis-card slide-in-up">
      <FileUpload @analysis-complete="handleAnalysisComplete" />
    </div>
    
    <div class="supported-collectors analysis-card slide-in-up">
      <div class="card-title">
        <el-icon><Setting /></el-icon>
        支持的GC收集器
      </div>
      <div class="collectors-list">
        <el-tag 
          v-for="collector in collectors" 
          :key="collector" 
          type="info" 
          size="large"
          effect="plain"
        >
          {{ collector }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Setting, TrendCharts, DataAnalysis, Document, Warning } from '@element-plus/icons-vue'
import FileUpload from '../components/FileUpload.vue'
import { getSupportedCollectors } from '../api/gcAnalysis'

const router = useRouter()
const collectors = ref(['G1GC', 'ZGC', 'CMS', 'Parallel GC', 'Serial GC', 'Shenandoah'])

const features = [
  {
    icon: 'TrendCharts',
    color: '#409EFF',
    title: '性能可视化',
    desc: '直观的图表展示GC性能趋势和内存使用情况'
  },
  {
    icon: 'DataAnalysis',
    color: '#67C23A',
    title: 'KPI分析',
    desc: '详细的吞吐量、延迟和并发时间等关键指标'
  },
  {
    icon: 'Document',
    color: '#E6A23C',
    title: '多格式支持',
    desc: '兼容各类JVM垃圾收集器的日志格式'
  },
  {
    icon: 'Warning',
    color: '#F56C6C',
    title: '智能诊断',
    desc: '自动识别问题并提供优化建议'
  }
]

onMounted(async () => {
  try {
    const response = await getSupportedCollectors()
    if (response.collectors) {
      collectors.value = response.collectors
    }
  } catch (error) {
    console.error('Failed to load collectors:', error)
  }
})

function handleAnalysisComplete(result) {
  // 跳转到分析结果页面
  router.push({
    name: 'AnalysisResult',
    params: { result: JSON.stringify(result) }
  })
  
  // 使用sessionStorage传递数据
  sessionStorage.setItem('gcAnalysisResult', JSON.stringify(result))
}
</script>

<style lang="scss" scoped>
.home-view {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-section {
  text-align: center;
  margin-bottom: 40px;
  
  h2 {
    font-size: 36px;
    font-weight: 700;
    color: white;
    margin-bottom: 16px;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }
  
  .welcome-desc {
    font-size: 18px;
    color: rgba(255, 255, 255, 0.9);
    margin-bottom: 48px;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  }
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
  
  .feature-item {
    background: white;
    padding: 32px 24px;
    border-radius: 12px;
    text-align: center;
    transition: all 0.3s ease;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
    }
    
    .el-icon {
      margin-bottom: 16px;
    }
    
    h3 {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 12px;
    }
    
    p {
      font-size: 14px;
      color: #606266;
      line-height: 1.6;
    }
  }
}

.upload-section {
  animation-delay: 0.1s;
}

.supported-collectors {
  animation-delay: 0.2s;
  
  .collectors-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    
    .el-tag {
      font-size: 14px;
      padding: 8px 16px;
    }
  }
}

@media (max-width: 768px) {
  .welcome-section {
    h2 {
      font-size: 28px;
    }
    
    .welcome-desc {
      font-size: 16px;
    }
  }
  
  .features-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}
</style>

