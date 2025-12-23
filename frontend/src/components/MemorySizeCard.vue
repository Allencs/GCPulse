<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><Files /></el-icon>
      JVM 内存大小
    </div>
    
    <div class="memory-table">
      <el-table :data="memoryData" style="width: 100%" stripe>
        <el-table-column prop="region" label="区域" width="150" />
        <el-table-column prop="allocated" label="分配内存">
          <template #default="scope">
            <el-tag type="info">{{ scope.row.allocated }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="peak" label="峰值使用">
          <template #default="scope">
            <el-tag type="warning">{{ scope.row.peak }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <div class="memory-chart" ref="memoryChartRef"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { Files } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const props = defineProps({
  memorySize: {
    type: Object,
    default: () => ({})
  }
})

const memoryChartRef = ref(null)

const memoryData = computed(() => {
  const { heap, metaspace, total } = props.memorySize || {}
  
  return [
    {
      region: 'Heap',
      allocated: heap?.allocatedFormatted || 'N/A',
      peak: heap?.peakFormatted || 'N/A'
    },
    {
      region: 'Metaspace',
      allocated: metaspace?.allocatedFormatted || 'N/A',
      peak: metaspace?.peakFormatted || 'N/A'
    },
    {
      region: 'Total',
      allocated: total?.allocatedFormatted || 'N/A',
      peak: total?.peakFormatted || 'N/A'
    }
  ]
})

onMounted(() => {
  initChart()
})

function initChart() {
  if (!memoryChartRef.value) return
  
  const chart = echarts.init(memoryChartRef.value)
  
  const { heap, metaspace } = props.memorySize || {}
  
  const option = {
    title: {
      text: 'JVM内存分配 vs 峰值使用',
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
      }
    },
    legend: {
      data: ['Heap', 'Metaspace'],
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['分配内存', '峰值使用']
    },
    yAxis: {
      type: 'value',
      name: 'GB',
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: [
      {
        name: 'Heap',
        type: 'bar',
        data: [
          (heap?.allocated || 0) / (1024 * 1024 * 1024),
          (heap?.peak || 0) / (1024 * 1024 * 1024)
        ],
        itemStyle: {
          color: '#409EFF'
        }
      },
      {
        name: 'Metaspace',
        type: 'bar',
        data: [
          (metaspace?.allocated || 0) / (1024 * 1024 * 1024),
          (metaspace?.peak || 0) / (1024 * 1024 * 1024)
        ],
        itemStyle: {
          color: '#67C23A'
        }
      }
    ]
  }
  
  chart.setOption(option)
  
  // 响应式
  window.addEventListener('resize', () => {
    chart.resize()
  })
}
</script>

<style lang="scss" scoped>
.memory-table {
  margin-bottom: 24px;
}

.memory-chart {
  width: 100%;
  height: 350px;
}
</style>

