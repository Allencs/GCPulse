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
import { ref, onMounted, watch } from 'vue'
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
  initHeapChart()
  initPauseChart()
})

watch(activeTab, (newTab) => {
  if (newTab === 'heap' && heapChart) {
    heapChart.resize()
  } else if (newTab === 'pause' && pauseChart) {
    pauseChart.resize()
  }
})

function initHeapChart() {
  if (!heapChartRef.value) return
  
  heapChart = echarts.init(heapChartRef.value)
  
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
      name: '时间 (秒)',
      axisLabel: {
        formatter: (value) => {
          return (value / 1000).toFixed(0)
        }
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
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    }
  }
  
  heapChart.setOption(option)
  
  window.addEventListener('resize', () => {
    heapChart?.resize()
  })
}

function initPauseChart() {
  if (!pauseChartRef.value) return
  
  pauseChart = echarts.init(pauseChartRef.value)
  
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
      name: '时间 (秒)',
      axisLabel: {
        formatter: (value) => {
          return (value / 1000).toFixed(0)
        }
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
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    }
  }
  
  pauseChart.setOption(option)
  
  window.addEventListener('resize', () => {
    pauseChart?.resize()
  })
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

