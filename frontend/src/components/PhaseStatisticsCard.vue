<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><DataLine /></el-icon>
      GC阶段统计
    </div>
    
    <div v-if="hasPhaseData" class="phase-content">
      <div class="phase-chart" ref="chartRef"></div>
      
      <el-table :data="phaseTableData" style="width: 100%" stripe>
        <el-table-column prop="phaseName" label="阶段名称" width="200" />
        <el-table-column prop="avgTime" label="平均时间" width="120">
          <template #default="scope">
            {{ formatTime(scope.row.avgTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="count" label="执行次数" width="100" />
        <el-table-column prop="totalTime" label="总时间">
          <template #default="scope">
            {{ formatDuration(scope.row.totalTime) }}
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <el-empty v-else description="暂无阶段统计数据" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { DataLine } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps({
  phaseStatistics: {
    type: Object,
    default: () => ({})
  }
})

const chartRef = ref(null)

const hasPhaseData = computed(() => {
  return Object.keys(props.phaseStatistics || {}).length > 0
})

const phaseTableData = computed(() => {
  if (!props.phaseStatistics) return []
  return Object.values(props.phaseStatistics)
})

onMounted(() => {
  if (hasPhaseData.value) {
    initChart()
  }
})

function initChart() {
  if (!chartRef.value) return
  
  const chart = echarts.init(chartRef.value)
  const phaseData = Object.values(props.phaseStatistics || {})
  
  const option = {
    title: {
      text: 'GC阶段平均时间分布',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 500
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.name}<br/>平均时间: ${params.value.toFixed(2)} ms<br/>占比: ${params.percent}%`
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle'
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
          formatter: '{b}: {d}%'
        },
        data: phaseData.map(phase => ({
          name: phase.phaseName,
          value: phase.avgTime || 0
        }))
      }
    ]
  }
  
  chart.setOption(option)
  
  window.addEventListener('resize', () => {
    chart.resize()
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
.phase-content {
  .phase-chart {
    width: 100%;
    height: 400px;
    margin-bottom: 24px;
  }
}
</style>

