<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><DataLine /></el-icon>
      GC 阶段统计
      <span class="subtitle">（Phase Statistics）</span>
    </div>
    
    <div v-if="hasPhaseData" class="phase-content">
      <!-- 双图表布局 -->
      <el-row :gutter="20" class="charts-row">
        <el-col :xs="24" :md="12">
          <div class="chart-container">
            <h4 class="chart-title">平均时间 (Avg Time)</h4>
            <div class="chart" ref="avgChartRef"></div>
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="chart-container">
            <h4 class="chart-title">累计时间占比 (Cumulative Time)</h4>
            <div class="chart" ref="pieChartRef"></div>
          </div>
        </el-col>
      </el-row>
      
      <!-- 详细统计表格 -->
      <el-table :data="phaseTableData" style="width: 100%" stripe border>
        <el-table-column prop="phaseName" label="阶段名称" min-width="180" fixed />
        <el-table-column label="总时间 (Total Time)" min-width="140">
          <template #default="scope">
            {{ formatDuration(scope.row.totalTime) }}
          </template>
        </el-table-column>
        <el-table-column label="平均时间 (Avg Time)" min-width="120">
          <template #default="scope">
            {{ formatTime(scope.row.avgTime) }}
          </template>
        </el-table-column>
        <el-table-column label="标准差 (Std Dev)" min-width="120">
          <template #default="scope">
            {{ formatTime(scope.row.stdDevTime) }}
          </template>
        </el-table-column>
        <el-table-column label="最小时间 (Min Time)" min-width="120">
          <template #default="scope">
            {{ formatTime(scope.row.minTime) }}
          </template>
        </el-table-column>
        <el-table-column label="最大时间 (Max Time)" min-width="120">
          <template #default="scope">
            {{ formatTime(scope.row.maxTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="count" label="执行次数 (Count)" width="110" align="center" />
      </el-table>
    </div>
    
    <el-empty v-else description="暂无阶段统计数据" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue'
import { DataLine } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps({
  phaseStatistics: {
    type: Object,
    default: () => ({})
  }
})

const avgChartRef = ref(null)
const pieChartRef = ref(null)
let avgChart = null
let pieChart = null

const hasPhaseData = computed(() => {
  return Object.keys(props.phaseStatistics || {}).length > 0
})

const phaseTableData = computed(() => {
  if (!props.phaseStatistics) return []
  return Object.values(props.phaseStatistics).sort((a, b) => b.totalTime - a.totalTime)
})

onMounted(() => {
  if (hasPhaseData.value) {
    nextTick(() => {
      initCharts()
    })
  }
})

watch(() => props.phaseStatistics, (newData) => {
  if (newData && Object.keys(newData).length > 0) {
    nextTick(() => {
      initCharts()
    })
  }
}, { deep: true })

function initCharts() {
  initAvgChart()
  initPieChart()
}

function initAvgChart() {
  if (!avgChartRef.value) return
  
  if (!avgChart) {
    avgChart = echarts.init(avgChartRef.value)
  }
  
  const phaseData = Object.values(props.phaseStatistics || {}).sort((a, b) => b.avgTime - a.avgTime)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params) => {
        const param = params[0]
        return `${param.name}<br/>平均时间: ${param.value.toFixed(3)} ms`
      }
    },
    grid: {
      left: '3%',
      right: '15%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '时间 (ms)',
      axisLabel: {
        formatter: '{value}'
      }
    },
    yAxis: {
      type: 'category',
      data: phaseData.map(phase => phase.phaseName),
      axisLabel: {
        fontSize: 11,
        interval: 0
      }
    },
    series: [
      {
        name: '平均时间',
        type: 'bar',
        data: phaseData.map(phase => phase.avgTime || 0),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#67C23A' }
          ])
        },
        label: {
          show: true,
          position: 'right',
          formatter: (params) => `${params.value.toFixed(2)} ms`,
          fontSize: 10
        }
      }
    ]
  }
  
  avgChart.setOption(option, true)
  
  window.addEventListener('resize', () => {
    avgChart?.resize()
  })
}

function initPieChart() {
  if (!pieChartRef.value) return
  
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }
  
  const phaseData = Object.values(props.phaseStatistics || {})
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const totalSeconds = (params.value / 1000).toFixed(2)
        return `${params.name}<br/>累计时间: ${totalSeconds} s<br/>占比: ${params.percent}%`
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      textStyle: {
        fontSize: 11
      },
      formatter: (name) => {
        const maxLength = 25
        return name.length > maxLength ? name.substring(0, maxLength) + '...' : name
      }
    },
    series: [
      {
        name: 'GC阶段',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{d}%',
          fontSize: 11
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        data: phaseData.map(phase => ({
          name: phase.phaseName,
          value: phase.totalTime || 0
        }))
      }
    ]
  }
  
  pieChart.setOption(option, true)
  
  window.addEventListener('resize', () => {
    pieChart?.resize()
  })
}

function formatTime(ms) {
  if (ms === null || ms === undefined) return 'N/A'
  if (ms < 1) {
    return (ms * 1000).toFixed(3) + ' μs'
  }
  return ms.toFixed(3) + ' ms'
}

function formatDuration(ms) {
  if (ms === null || ms === undefined) return 'N/A'
  
  // 如果小于1秒，显示毫秒
  if (ms < 1000) {
    return ms.toFixed(3) + ' ms'
  }
  
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  
  if (minutes > 0) {
    const remainingSeconds = seconds % 60
    const remainingMs = Math.floor(ms % 1000)
    return `${minutes}m ${remainingSeconds}.${remainingMs.toString().padStart(3, '0')}s`
  } else {
    const remainingMs = (ms % 1000).toFixed(0)
    return `${seconds}.${remainingMs.padStart(3, '0')}s`
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

.phase-content {
  .charts-row {
    margin-bottom: 24px;
    
    .chart-container {
      background: #fff;
      border: 1px solid #e4e7ed;
      border-radius: 8px;
      padding: 16px;
      
      .chart-title {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin: 0 0 12px 0;
        text-align: center;
      }
      
      .chart {
        width: 100%;
        height: 350px;
      }
    }
  }
  
  .el-table {
    margin-top: 20px;
  }
}

@media (max-width: 768px) {
  .phase-content {
    .charts-row {
      .chart-container {
        margin-bottom: 16px;
        
        .chart {
          height: 300px;
        }
      }
    }
  }
}
</style>

