<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><Warning /></el-icon>
      GC 原因统计
      <span class="subtitle">（导致 GC 的原因及耗时统计）</span>
    </div>
    
    <div v-if="gcCauses && Object.keys(gcCauses).length > 0" class="gc-causes-content">
      <div class="chart-container" ref="chartRef"></div>
      
      <el-table :data="tableData" stripe style="width: 100%; margin-top: 20px">
        <el-table-column prop="cause" label="GC 原因" min-width="200" />
        <el-table-column prop="count" label="次数" width="100" align="center" sortable />
        <el-table-column prop="avgTime" label="平均时间" width="120" align="center" sortable>
          <template #default="scope">
            {{ formatTime(scope.row.avgTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="maxTime" label="最大时间" width="120" align="center" sortable>
          <template #default="scope">
            {{ formatTime(scope.row.maxTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalTime" label="总时间" width="150" align="center" sortable>
          <template #default="scope">
            {{ formatTotalTime(scope.row.totalTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="percentage" label="占比" width="100" align="center" sortable>
          <template #default="scope">
            {{ scope.row.percentage.toFixed(2) }}%
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <el-empty v-else description="未找到 GC 原因统计数据" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { Warning } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps({
  gcCauses: {
    type: Object,
    default: () => ({})
  }
})

const chartRef = ref(null)

const tableData = computed(() => {
  if (!props.gcCauses) return []
  return Object.values(props.gcCauses).sort((a, b) => b.totalTime - a.totalTime)
})

onMounted(() => {
  if (tableData.value.length > 0) {
    initChart()
  }
})

function initChart() {
  if (!chartRef.value) return
  
  const chart = echarts.init(chartRef.value)
  
  const option = {
    title: {
      text: 'GC 原因时间占比',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 500
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const data = params.data
        return `${data.name}<br/>总时间: ${formatTotalTime(data.value)}<br/>占比: ${params.percent}%<br/>次数: ${data.count}`
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle'
    },
    series: [
      {
        name: 'GC 原因',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        data: tableData.value.map(cause => ({
          name: cause.cause,
          value: cause.totalTime,
          count: cause.count
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
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
  return ms.toFixed(3) + ' ms'
}

function formatTotalTime(ms) {
  if (ms === null || ms === undefined) return 'N/A'
  if (ms >= 1000) {
    return (ms / 1000).toFixed(3) + ' s'
  }
  return ms.toFixed(3) + ' ms'
}
</script>

<style lang="scss" scoped>
.gc-causes-content {
  .chart-container {
    width: 100%;
    height: 400px;
    margin-bottom: 20px;
  }
  
  .subtitle {
    font-size: 12px;
    color: #909399;
    font-weight: normal;
    margin-left: 10px;
  }
}
</style>

