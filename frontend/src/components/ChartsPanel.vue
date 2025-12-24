<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><TrendCharts /></el-icon>
      交互式图表
    </div>
    
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="堆内存使用趋势" name="heap">
        <div class="chart-container" ref="heapChartRef"></div>
      </el-tab-pane>
      
      <el-tab-pane label="GC暂停时间趋势" name="pause">
        <div class="chart-container" ref="pauseChartRef"></div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps({
  timeSeriesData: {
    type: Object,
    default: () => ({})
  }
})

const activeTab = ref('heap')
const heapChartRef = ref(null)
const pauseChartRef = ref(null)
let heapChart = null
let pauseChart = null

onMounted(() => {
  nextTick(() => {
  initHeapChart()
  initPauseChart()
})
})

// 监听数据变化，重新渲染图表
watch(() => props.timeSeriesData, (newData) => {
  if (newData && Object.keys(newData).length > 0) {
    nextTick(() => {
      updateHeapChart()
      updatePauseChart()
    })
  }
}, { deep: true })

watch(activeTab, (newTab) => {
  nextTick(() => {
  if (newTab === 'heap' && heapChart) {
    heapChart.resize()
  } else if (newTab === 'pause' && pauseChart) {
    pauseChart.resize()
  }
  })
})

// 通用时间格式化函数
function formatTimeAxis(value) {
  // 判断是否为绝对Unix时间戳
  // 方法：转换成日期后检查年份是否在合理范围内（2000-2100年）
  const date = new Date(value)
  const year = date.getFullYear()
  const isAbsoluteTime = year >= 2000 && year <= 2100
  
  if (isAbsoluteTime) {
    // 绝对时间：显示标准日期时间格式
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    const seconds = date.getSeconds().toString().padStart(2, '0')
    return `${month}-${day} ${hours}:${minutes}:${seconds}`
  } else {
    // 相对时间：显示GC运行时间
    const totalSeconds = Math.floor(value / 1000)
    const hours = Math.floor(totalSeconds / 3600)
    const minutes = Math.floor((totalSeconds % 3600) / 60)
    const seconds = totalSeconds % 60
    
    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
    } else if (minutes > 0) {
      return `${minutes}:${seconds.toString().padStart(2, '0')}`
    } else {
      return `${seconds}s`
    }
  }
}

function initHeapChart() {
  if (!heapChartRef.value) return
  
  if (!heapChart) {
  heapChart = echarts.init(heapChartRef.value)
  }
  
  updateHeapChart()
  
  window.addEventListener('resize', () => {
    heapChart?.resize()
  })
}

function updateHeapChart() {
  if (!heapChart) return
  
  const heapData = props.timeSeriesData?.heapUsageTrend || []
  
  const option = {
    title: {
      text: '堆内存使用趋势',
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
        return `时间: ${formatTimestamp(param.data[0])}<br/>内存: ${param.data[1].toFixed(3)} MB`
      }
    },
    xAxis: {
      type: 'time',
      axisLabel: {
        formatter: formatTimeAxis,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '内存使用 (MB)',
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: [
      {
        name: '堆内存',
        type: 'line',
        data: heapData.map(d => [d.timestamp, d.value]),
        smooth: true,
        lineStyle: {
          color: '#409EFF',
          width: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        }
      }
    ],
    grid: {
      left: '3%',
      right: '8%',
      bottom: '12%',
      top: '12%',
      containLabel: true
    }
  }
  
  heapChart.setOption(option, true)  // true = 不合并，完全替换
}

function initPauseChart() {
  if (!pauseChartRef.value) return
  
  if (!pauseChart) {
  pauseChart = echarts.init(pauseChartRef.value)
  }
  
  updatePauseChart()
  
  window.addEventListener('resize', () => {
    pauseChart?.resize()
  })
}

function updatePauseChart() {
  if (!pauseChart) return
  
  const pauseData = props.timeSeriesData?.pauseTimeTrend || []
  
  const option = {
    title: {
      text: 'GC暂停时间趋势',
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
        return `时间: ${formatTimestamp(param.data[0])}<br/>暂停: ${param.data[1].toFixed(3)} ms`
      }
    },
    xAxis: {
      type: 'time',
      axisLabel: {
        formatter: formatTimeAxis,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '暂停时间 (ms)',
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: [
      {
        name: 'GC暂停',
        type: 'scatter',
        data: pauseData.map(d => [d.timestamp, d.value]),
        symbolSize: 8,
        itemStyle: {
          color: '#E6A23C'
        }
      }
    ],
    grid: {
      left: '3%',
      right: '8%',
      bottom: '12%',
      top: '12%',
      containLabel: true
    }
  }
  
  pauseChart.setOption(option, true)  // true = 不合并，完全替换
}

function formatTimestamp(ms) {
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  if (hours > 0) {
    return `${hours}h ${minutes % 60}m ${seconds % 60}s`
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`
  } else {
    return `${seconds}s`
  }
}
</script>

<style lang="scss" scoped>
.chart-container {
  width: 100%;
  height: 400px;
  margin-top: 20px;
}
</style>

