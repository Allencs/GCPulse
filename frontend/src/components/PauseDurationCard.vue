<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><Histogram /></el-icon>
      GC暂停时间分布
    </div>
    
    <div class="duration-chart" ref="chartRef"></div>
    
    <div class="duration-table">
      <el-table :data="distributionData" style="width: 100%" stripe>
        <el-table-column prop="rangeLabel" label="时间范围" width="150" />
        <el-table-column prop="count" label="GC次数" width="120" />
        <el-table-column prop="percentage" label="占比">
          <template #default="scope">
            <el-progress 
              :percentage="scope.row.percentage" 
              :color="getProgressColor(scope.row.percentage)"
              :format="(percentage) => percentage.toFixed(2) + '%'"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { Histogram } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps({
  pauseDistribution: {
    type: Object,
    default: () => ({})
  }
})

const chartRef = ref(null)

const distributionData = computed(() => {
  const ranges = props.pauseDistribution?.ranges || []
  return ranges.filter(r => r.count > 0)
})

onMounted(() => {
  initChart()
})

function initChart() {
  if (!chartRef.value) return
  
  const chart = echarts.init(chartRef.value)
  const ranges = props.pauseDistribution?.ranges || []
  
  const option = {
    title: {
      text: 'GC暂停时间分布',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 500
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params) => {
        const param = params[0]
        return `${param.name}<br/>次数: ${param.value}<br/>占比: ${param.data.percentage.toFixed(3)}%`
      }
    },
    xAxis: {
      type: 'category',
      data: ranges.map(r => r.rangeLabel),
      axisLabel: {
        rotate: 45,
        interval: 0
      }
    },
    yAxis: {
      type: 'value',
      name: 'GC次数'
    },
    series: [
      {
        name: 'GC次数',
        type: 'bar',
        data: ranges.map(r => ({
          value: r.count,
          percentage: r.percentage,
          itemStyle: {
            color: r.percentage > 50 ? '#67C23A' : '#409EFF'
          }
        })),
        barWidth: '60%'
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '15%',
      containLabel: true
    }
  }
  
  chart.setOption(option)
  
  window.addEventListener('resize', () => {
    chart.resize()
  })
}

function getProgressColor(percentage) {
  if (percentage > 50) return '#67C23A'
  if (percentage > 20) return '#409EFF'
  return '#E6A23C'
}
</script>

<style lang="scss" scoped>
.duration-chart {
  width: 100%;
  height: 350px;
  margin-bottom: 24px;
}

.duration-table {
  margin-top: 20px;
}
</style>

