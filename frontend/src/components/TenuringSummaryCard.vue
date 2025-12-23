<template>
  <div v-if="tenuringSummary" class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><TrendCharts /></el-icon>
      老年代晋升总结
    </div>
    
    <div class="tenuring-content">
      <div class="summary-grid">
        <div class="summary-item">
          <div class="label">最大晋升阈值</div>
          <div class="value">{{ tenuringSummary.maxTenuringThreshold || 'N/A' }}</div>
        </div>
        <div class="summary-item">
          <div class="label">平均晋升阈值</div>
          <div class="value">{{ tenuringSummary.avgTenuringThreshold || 'N/A' }}</div>
        </div>
        <div class="summary-item">
          <div class="label">总存活对象</div>
          <div class="value">{{ formatBytes(tenuringSummary.totalSurvivedObjects) }}</div>
        </div>
        <div class="summary-item">
          <div class="label">晋升率</div>
          <div class="value">{{ tenuringSummary.promotionRate?.toFixed(2) || '0.00' }}%</div>
        </div>
      </div>
      
      <div v-if="tenuringSummary.ageDistribution && Object.keys(tenuringSummary.ageDistribution).length > 0" 
           class="chart-container" 
           ref="chartRef"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps({
  tenuringSummary: {
    type: Object,
    default: () => null
  }
})

const chartRef = ref(null)

const hasAgeData = computed(() => {
  return props.tenuringSummary?.ageDistribution && 
         Object.keys(props.tenuringSummary.ageDistribution).length > 0
})

onMounted(() => {
  if (hasAgeData.value) {
    initChart()
  }
})

function initChart() {
  if (!chartRef.value) return
  
  const chart = echarts.init(chartRef.value)
  const ageData = props.tenuringSummary.ageDistribution
  
  const ages = Object.keys(ageData).map(Number).sort((a, b) => a - b)
  const values = ages.map(age => ageData[age])
  
  const option = {
    title: {
      text: '对象年龄分布',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 500
      }
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const param = params[0]
        return `年龄 ${param.name}: ${formatBytes(param.value)}`
      }
    },
    xAxis: {
      type: 'category',
      data: ages.map(age => `Age ${age}`),
      name: '对象年龄'
    },
    yAxis: {
      type: 'value',
      name: '字节数',
      axisLabel: {
        formatter: (value) => formatBytes(value)
      }
    },
    series: [
      {
        data: values,
        type: 'bar',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#79bbff' }
          ])
        }
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    }
  }
  
  chart.setOption(option)
  
  window.addEventListener('resize', () => {
    chart.resize()
  })
}

function formatBytes(bytes) {
  if (!bytes) return '0 B'
  if (bytes >= 1024 * 1024) {
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  } else if (bytes >= 1024) {
    return (bytes / 1024).toFixed(2) + ' KB'
  }
  return bytes + ' B'
}
</script>

<style lang="scss" scoped>
.tenuring-content {
  .summary-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 30px;
    
    .summary-item {
      text-align: center;
      padding: 20px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 8px;
      color: white;
      
      .label {
        font-size: 14px;
        margin-bottom: 10px;
        opacity: 0.9;
      }
      
      .value {
        font-size: 24px;
        font-weight: bold;
      }
    }
  }
  
  .chart-container {
    width: 100%;
    height: 300px;
    margin-top: 20px;
  }
}
</style>

