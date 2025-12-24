<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><TrendCharts /></el-icon>
      交互式图表
      <span class="subtitle">（多视图数据分析）</span>
    </div>
    
    <!-- 时间范围选择器 -->
    <div class="time-range-selector">
      <el-row :gutter="16" align="middle">
        <el-col :span="18">
          <el-tooltip content="选择特定的时间范围进行分析" placement="top">
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="x"
              :disabled="!hasData"
            />
          </el-tooltip>
        </el-col>
        <el-col :span="6">
          <el-tooltip content="清除时间范围过滤，显示完整数据" placement="top">
            <el-button type="danger" @click="resetTimeRange" :disabled="!hasData">
              重置
            </el-button>
          </el-tooltip>
        </el-col>
      </el-row>
    </div>
    
    <!-- 视图切换按钮 -->
    <div class="view-selector">
      <el-button-group>
        <el-tooltip content="显示每次 GC 执行后的堆内存使用情况" placement="top">
          <el-button 
            :type="activeView === 'heapAfter' ? 'danger' : 'default'"
            @click="switchView('heapAfter')"
          >
            Heap after GC
          </el-button>
        </el-tooltip>
        <el-tooltip content="显示每次 GC 执行前的堆内存使用情况" placement="top">
          <el-button 
            :type="activeView === 'heapBefore' ? 'danger' : 'default'"
            @click="switchView('heapBefore')"
          >
            Heap before GC
          </el-button>
        </el-tooltip>
        <el-tooltip content="显示每次 GC 的暂停时间（STW 时间）" placement="top">
          <el-button 
            :type="activeView === 'duration' ? 'danger' : 'default'"
            @click="switchView('duration')"
          >
            GC Duration
          </el-button>
        </el-tooltip>
        <el-tooltip content="显示每次 GC 回收的内存量（GC 前后的内存差值）" placement="top">
          <el-button 
            :type="activeView === 'reclaimed' ? 'danger' : 'default'"
            @click="switchView('reclaimed')"
          >
            Reclaimed Bytes
          </el-button>
        </el-tooltip>
        <el-tooltip 
          :content="hasYoungGenData ? '显示年轻代（Young Generation）的内存使用趋势' : '当前日志不包含年轻代数据'"
          placement="top"
        >
          <el-button 
            :type="activeView === 'youngGen' ? 'danger' : 'default'"
            @click="switchView('youngGen')"
            :disabled="!hasYoungGenData"
          >
            Young Gen
          </el-button>
        </el-tooltip>
        <el-tooltip 
          :content="hasOldGenData ? '显示老年代（Old Generation）的内存使用趋势' : '当前日志不包含老年代数据'"
          placement="top"
        >
          <el-button 
            :type="activeView === 'oldGen' ? 'danger' : 'default'"
            @click="switchView('oldGen')"
            :disabled="!hasOldGenData"
          >
            Old Gen
          </el-button>
        </el-tooltip>
        <el-tooltip 
          :content="hasAllocationData ? '显示对象分配（Allocation）和晋升（Promotion）趋势' : '当前日志不包含分配和晋升数据'"
          placement="top"
        >
          <el-button 
            :type="activeView === 'allocation' ? 'danger' : 'default'"
            @click="switchView('allocation')"
            :disabled="!hasAllocationData"
          >
            A & P
          </el-button>
        </el-tooltip>
      </el-button-group>
    </div>
    
    <!-- 图表容器 -->
    <div class="chart-container" ref="chartRef"></div>
    
    <!-- GC 统计信息 -->
    <div class="gc-statistics">
      <h4>GC 统计信息</h4>
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="stat-card">
            <div class="stat-chart" ref="reclaimedChartRef"></div>
            <div class="stat-label">总回收字节数</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-card">
            <div class="stat-chart" ref="cumulativeChartRef"></div>
            <div class="stat-label">GC 累计时间</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-card">
            <div class="stat-chart" ref="avgTimeChartRef"></div>
            <div class="stat-label">GC 平均时间</div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps({
  timeSeriesData: {
    type: Object,
    default: () => ({})
  },
  gcEvents: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
const reclaimedChartRef = ref(null)
const cumulativeChartRef = ref(null)
const avgTimeChartRef = ref(null)

const activeView = ref('heapAfter')
const timeRange = ref(null)

let mainChart = null
let reclaimedChart = null
let cumulativeChart = null
let avgTimeChart = null

const hasData = computed(() => {
  return props.timeSeriesData && Object.keys(props.timeSeriesData).length > 0
})

const hasYoungGenData = computed(() => {
  return props.timeSeriesData?.youngGenTrend && props.timeSeriesData.youngGenTrend.length > 0
})

const hasOldGenData = computed(() => {
  return props.timeSeriesData?.oldGenTrend && props.timeSeriesData.oldGenTrend.length > 0
})

const hasAllocationData = computed(() => {
  return (props.timeSeriesData?.allocationTrend && props.timeSeriesData.allocationTrend.length > 0) ||
         (props.timeSeriesData?.promotionTrend && props.timeSeriesData.promotionTrend.length > 0)
})

onMounted(() => {
  nextTick(() => {
    initCharts()
  })
})

// 监听数据变化
watch(() => props.timeSeriesData, (newData) => {
  if (newData && Object.keys(newData).length > 0) {
    nextTick(() => {
      updateAllCharts()
    })
  }
}, { deep: true })

function initCharts() {
  if (chartRef.value) {
    mainChart = echarts.init(chartRef.value)
  }
  if (reclaimedChartRef.value) {
    reclaimedChart = echarts.init(reclaimedChartRef.value)
  }
  if (cumulativeChartRef.value) {
    cumulativeChart = echarts.init(cumulativeChartRef.value)
  }
  if (avgTimeChartRef.value) {
    avgTimeChart = echarts.init(avgTimeChartRef.value)
  }
  
  updateAllCharts()
  
  window.addEventListener('resize', () => {
    mainChart?.resize()
    reclaimedChart?.resize()
    cumulativeChart?.resize()
    avgTimeChart?.resize()
  })
}

function updateAllCharts() {
  updateMainChart()
  updateStatCharts()
}

function switchView(view) {
  activeView.value = view
  updateMainChart()
}

function resetTimeRange() {
  timeRange.value = null
  updateMainChart()
}

function getFilteredData(data) {
  if (!data || data.length === 0) return []
  if (!timeRange.value || timeRange.value.length !== 2) return data
  
  const [start, end] = timeRange.value
  return data.filter(d => d.timestamp >= start && d.timestamp <= end)
}

function updateMainChart() {
  if (!mainChart || !hasData.value) return
  
  let chartData = []
  let title = ''
  let yAxisName = ''
  let seriesName = ''
  let color = '#409EFF'
  let chartType = 'line'
  let series = []
  
  switch (activeView.value) {
    case 'heapAfter':
      chartData = getFilteredData(props.timeSeriesData.heapUsageTrend)
      title = 'Heap Memory After GC'
      yAxisName = '内存 (MB)'
      seriesName = 'Heap After GC'
      color = '#409EFF'
      break
    case 'heapBefore':
      chartData = getFilteredData(props.timeSeriesData.heapBeforeGCTrend)
      title = 'Heap Memory Before GC'
      yAxisName = '内存 (MB)'
      seriesName = 'Heap Before GC'
      color = '#E6A23C'
      break
    case 'duration':
      chartData = getFilteredData(props.timeSeriesData.pauseTimeTrend)
      title = 'GC Duration'
      yAxisName = '时间 (ms)'
      seriesName = 'GC Duration'
      color = '#F56C6C'
      chartType = 'scatter'
      break
    case 'reclaimed':
      chartData = getFilteredData(props.timeSeriesData.reclaimedBytesTrend)
      title = 'Reclaimed Bytes'
      yAxisName = '字节数 (MB)'
      seriesName = 'Reclaimed'
      color = '#67C23A'
      chartType = 'bar'
      break
    case 'youngGen':
      chartData = getFilteredData(props.timeSeriesData.youngGenTrend)
      title = 'Young Generation'
      yAxisName = '内存 (MB)'
      seriesName = 'Young Gen'
      color = '#909399'
      break
    case 'oldGen':
      chartData = getFilteredData(props.timeSeriesData.oldGenTrend)
      title = 'Old Generation'
      yAxisName = '内存 (MB)'
      seriesName = 'Old Gen'
      color = '#C45656'
      break
    case 'allocation':
      // A & P: Allocation and Promotion
      const allocationData = getFilteredData(props.timeSeriesData.allocationTrend || [])
      const promotionData = getFilteredData(props.timeSeriesData.promotionTrend || [])
      
      series = [
        {
          name: '对象分配大小 (Allocation)',
          type: 'line',
          data: allocationData.map(d => [d.timestamp, d.value]),
          smooth: true,
          lineStyle: { color: '#67C23A', width: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
              { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
            ])
          }
        },
        {
          name: '对象晋升大小 (Promotion)',
          type: 'line',
          data: promotionData.map(d => [d.timestamp, d.value]),
          smooth: true,
          lineStyle: { color: '#F56C6C', width: 2 }
        }
      ]
      
      title = 'Allocation & Promotion'
      yAxisName = '字节数 (MB)'
      break
  }
  
  if (activeView.value !== 'allocation') {
    series = [{
      name: seriesName,
      type: chartType,
      data: chartData.map(d => [d.timestamp, d.value]),
      smooth: chartType === 'line',
      symbolSize: chartType === 'scatter' ? 8 : undefined,
      itemStyle: { color: color },
      lineStyle: chartType === 'line' ? { color: color, width: 2 } : undefined,
      areaStyle: chartType === 'line' ? {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: `${color}4D` },
          { offset: 1, color: `${color}0D` }
        ])
      } : undefined
    }]
  }
  
  const option = {
    title: {
      text: title,
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 500
      }
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        if (!params || params.length === 0) return ''
        let result = `时间: ${formatTimestamp(params[0].data[0])}<br/>`
        params.forEach(param => {
          result += `${param.seriesName}: ${param.data[1].toFixed(3)}<br/>`
        })
        return result
      }
    },
    legend: activeView.value === 'allocation' ? {
      data: ['对象分配大小 (Allocation)', '对象晋升大小 (Promotion)'],
      top: '8%',
      left: 'center',
      itemGap: 20,
      textStyle: {
        fontSize: 12
      }
    } : undefined,
    xAxis: {
      type: 'time',
      axisLabel: {
        formatter: (value) => {
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
        },
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: series,
    grid: {
      left: '3%',
      right: '8%',
      bottom: '12%',
      top: activeView.value === 'allocation' ? '20%' : '12%',
      containLabel: true
    },
    dataZoom: [
      {
        type: 'slider',
        show: true,
        start: 0,
        end: 100
      },
      {
        type: 'inside',
        start: 0,
        end: 100
      }
    ]
  }
  
  mainChart.setOption(option, true)
}

function updateStatCharts() {
  // Reclaimed Bytes Chart
  if (reclaimedChart && props.gcEvents) {
    const minorGCEvents = props.gcEvents.filter(e => !e.isFullGC)
    const fullGCEvents = props.gcEvents.filter(e => e.isFullGC)
    
    const minorGCReclaimed = calculateTotalReclaimed(minorGCEvents)
    const fullGCReclaimed = calculateTotalReclaimed(fullGCEvents)
    
    // 后端返回的是字节数，转换为 MB
    const minorGCReclaimedMB = minorGCReclaimed / (1024 * 1024)
    const fullGCReclaimedMB = fullGCReclaimed / (1024 * 1024)
    
    // 智能选择单位：大于1024MB显示为GB，否则显示MB
    const formatSize = (mb) => {
      if (mb >= 1024) {
        return `${(mb / 1024).toFixed(2)} GB`
      } else {
        return `${mb.toFixed(2)} MB`
      }
    }
    
    const option = {
      series: [{
        type: 'bar',
        data: [
          { value: minorGCReclaimedMB, name: 'Minor GC', label: formatSize(minorGCReclaimedMB) },
          { value: fullGCReclaimedMB, name: 'Full GC', label: formatSize(fullGCReclaimedMB) }
        ],
        label: {
          show: true,
          position: 'top',
          formatter: (params) => params.data.label
        },
        itemStyle: {
          color: (params) => params.dataIndex === 0 ? '#409EFF' : '#909399'
        }
      }],
      xAxis: {
        type: 'category',
        data: ['Minor GC', 'Full GC'],
        axisLabel: { fontSize: 10 }
      },
      yAxis: {
        type: 'value',
        show: false
      },
      grid: {
        left: 0,
        right: 0,
        top: 20,
        bottom: 30
      }
    }
    reclaimedChart.setOption(option, true)
  }
  
  // Cumulative Time Chart
  if (cumulativeChart && props.gcEvents) {
    const minorGCTime = calculateTotalTime(props.gcEvents.filter(e => !e.isFullGC))
    const fullGCTime = calculateTotalTime(props.gcEvents.filter(e => e.isFullGC))
    
    const option = {
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: [
          { value: minorGCTime, name: 'Minor GC' },
          { value: fullGCTime, name: 'Full GC' }
        ],
        label: {
          show: false
        },
        itemStyle: {
          color: (params) => params.dataIndex === 0 ? '#409EFF' : '#909399'
        }
      }],
      legend: {
        bottom: 0,
        left: 'center',
        textStyle: { fontSize: 10 }
      }
    }
    cumulativeChart.setOption(option, true)
  }
  
  // Average Time Chart
  if (avgTimeChart && props.gcEvents) {
    const minorGCAvg = calculateAvgTime(props.gcEvents.filter(e => !e.isFullGC))
    const fullGCAvg = calculateAvgTime(props.gcEvents.filter(e => e.isFullGC))
    
    const option = {
      series: [{
        type: 'bar',
        data: [
          { value: minorGCAvg, name: 'Minor GC' },
          { value: fullGCAvg, name: 'Full GC' }
        ],
        label: {
          show: true,
          position: 'top',
          formatter: (params) => {
            return `${params.value.toFixed(2)} ms`
          }
        },
        itemStyle: {
          color: (params) => params.dataIndex === 0 ? '#409EFF' : '#909399'
        }
      }],
      xAxis: {
        type: 'category',
        data: ['Minor GC', 'Full GC'],
        axisLabel: { fontSize: 10 }
      },
      yAxis: {
        type: 'value',
        show: false
      },
      grid: {
        left: 0,
        right: 0,
        top: 20,
        bottom: 30
      }
    }
    avgTimeChart.setOption(option, true)
  }
}

function calculateTotalReclaimed(events) {
  return events.reduce((sum, e) => {
    if (e.heapMemory && e.heapMemory.before && e.heapMemory.after) {
      return sum + (e.heapMemory.before - e.heapMemory.after)
    }
    return sum
  }, 0)
}

function calculateTotalTime(events) {
  return events.reduce((sum, e) => sum + (e.pauseTime || 0), 0)
}

function calculateAvgTime(events) {
  if (events.length === 0) return 0
  return calculateTotalTime(events) / events.length
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
.subtitle {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
  margin-left: 10px;
}

.time-range-selector {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.view-selector {
  margin-bottom: 20px;
  
  .el-button-group {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    
    .el-button {
      flex: 1;
      min-width: 120px;
      margin: 0;
    }
  }
}

.chart-container {
  width: 100%;
  height: 450px;
  margin-bottom: 30px;
}

.gc-statistics {
  h4 {
    margin-bottom: 15px;
    color: #303133;
    font-size: 15px;
    font-weight: 500;
  }
  
  .stat-card {
    background: #fff;
    border: 1px solid #DCDFE6;
    border-radius: 8px;
    padding: 15px;
    text-align: center;
    
    .stat-chart {
      width: 100%;
      height: 150px;
      margin-bottom: 10px;
    }
    
    .stat-label {
      font-size: 13px;
      color: #606266;
      font-weight: 500;
    }
  }
}

@media (max-width: 768px) {
  .view-selector .el-button-group .el-button {
    min-width: 90px;
    font-size: 12px;
  }
}
</style>

