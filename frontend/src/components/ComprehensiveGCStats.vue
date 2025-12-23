<template>
  <div class="analysis-card slide-in-up">
    <div class="card-title">
      <el-icon><DataAnalysis /></el-icon>
      详细 GC 统计
    </div>
    
    <el-row :gutter="20">
      <!-- Total GC stats -->
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stats-card">
          <h3 class="stats-card-title">Total GC stats</h3>
          <div class="stats-table">
            <div class="stats-row">
              <span class="stats-label">Total GC count</span>
              <span class="stats-value">{{ totalStats.count }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Total reclaimed bytes</span>
              <span class="stats-value">{{ totalStats.totalReclaimed }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Total GC time</span>
              <span class="stats-value">{{ totalStats.totalTime }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Avg GC time</span>
              <span class="stats-value">{{ totalStats.avgTime }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">GC avg time std dev</span>
              <span class="stats-value">{{ totalStats.stdDev }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">GC min/max time</span>
              <span class="stats-value">{{ totalStats.minMax }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">GC Interval avg time</span>
              <span class="stats-value">{{ totalStats.intervalAvg }}</span>
            </div>
          </div>
        </div>
      </el-col>
      
      <!-- Minor GC stats -->
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stats-card">
          <h3 class="stats-card-title">Minor GC stats</h3>
          <div class="stats-table">
            <div class="stats-row">
              <span class="stats-label">Minor GC count</span>
              <span class="stats-value">{{ minorStats.count }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Minor GC reclaimed</span>
              <span class="stats-value">{{ minorStats.totalReclaimed }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Minor GC total time</span>
              <span class="stats-value">{{ minorStats.totalTime }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Minor GC avg time</span>
              <span class="stats-value">{{ minorStats.avgTime }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Minor GC avg time std dev</span>
              <span class="stats-value">{{ minorStats.stdDev }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Minor GC min/max time</span>
              <span class="stats-value">{{ minorStats.minMax }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Minor GC Interval avg</span>
              <span class="stats-value">{{ minorStats.intervalAvg }}</span>
            </div>
          </div>
        </div>
      </el-col>
      
      <!-- Full GC stats -->
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stats-card">
          <h3 class="stats-card-title">Full GC stats</h3>
          <div class="stats-table">
            <div class="stats-row">
              <span class="stats-label">Full GC Count</span>
              <span class="stats-value">{{ fullStats.count }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Full GC reclaimed</span>
              <span class="stats-value">{{ fullStats.totalReclaimed }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Full GC total time</span>
              <span class="stats-value">{{ fullStats.totalTime }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Full GC avg time</span>
              <span class="stats-value">{{ fullStats.avgTime }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Full GC avg time std dev</span>
              <span class="stats-value">{{ fullStats.stdDev }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Full GC min/max time</span>
              <span class="stats-value">{{ fullStats.minMax }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Full GC Interval avg</span>
              <span class="stats-value">{{ fullStats.intervalAvg }}</span>
            </div>
          </div>
        </div>
      </el-col>
      
      <!-- GC Pause Statistics -->
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stats-card">
          <h3 class="stats-card-title">GC Pause Statistics</h3>
          <div class="stats-table">
            <div class="stats-row">
              <span class="stats-label">Pause Count</span>
              <span class="stats-value">{{ pauseStats.count }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Pause total time</span>
              <span class="stats-value">{{ pauseStats.totalTime }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Pause avg time</span>
              <span class="stats-value">{{ pauseStats.avgTime }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Pause avg time std dev</span>
              <span class="stats-value">{{ pauseStats.stdDev }}</span>
            </div>
            <div class="stats-row">
              <span class="stats-label">Pause min/max time</span>
              <span class="stats-value">{{ pauseStats.minMax }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { DataAnalysis } from '@element-plus/icons-vue'

const props = defineProps({
  gcEvents: {
    type: Array,
    default: () => []
  },
  kpiMetrics: {
    type: Object,
    default: () => ({})
  }
})

// 计算总体 GC 统计
const totalStats = computed(() => {
  if (!props.gcEvents || props.gcEvents.length === 0) {
    return getEmptyStats()
  }
  
  const events = props.gcEvents
  const totalTime = events.reduce((sum, e) => sum + (e.pauseTime || 0), 0)
  const totalReclaimed = events.reduce((sum, e) => {
    if (e.heapMemory && e.heapMemory.before && e.heapMemory.after) {
      return sum + (e.heapMemory.before - e.heapMemory.after)
    }
    return sum
  }, 0)
  
  return {
    count: events.length,
    totalReclaimed: formatBytes(totalReclaimed),
    totalTime: formatDuration(totalTime),
    avgTime: formatTime(props.kpiMetrics?.latency?.avgPauseTime),
    stdDev: formatTime(props.kpiMetrics?.latency?.stdDevPauseTime),
    minMax: `${formatTime(props.kpiMetrics?.latency?.minPauseTime)} / ${formatTime(props.kpiMetrics?.latency?.maxPauseTime)}`,
    intervalAvg: calculateIntervalAvg(events)
  }
})

// 计算 Minor GC 统计
const minorStats = computed(() => {
  if (!props.gcEvents || props.gcEvents.length === 0) {
    return getEmptyStats()
  }
  
  const minorEvents = props.gcEvents.filter(e => !e.isFullGC)
  if (minorEvents.length === 0) {
    return getEmptyStats()
  }
  
  const times = minorEvents.map(e => e.pauseTime || 0)
  const totalTime = times.reduce((sum, t) => sum + t, 0)
  const totalReclaimed = minorEvents.reduce((sum, e) => {
    if (e.heapMemory && e.heapMemory.before && e.heapMemory.after) {
      return sum + (e.heapMemory.before - e.heapMemory.after)
    }
    return sum
  }, 0)
  
  const stats = calculateStats(times)
  
  return {
    count: minorEvents.length,
    totalReclaimed: formatBytes(totalReclaimed),
    totalTime: formatDuration(totalTime),
    avgTime: formatTime(stats.avg),
    stdDev: formatTime(stats.stdDev),
    minMax: `${formatTime(stats.min)} / ${formatTime(stats.max)}`,
    intervalAvg: calculateIntervalAvg(minorEvents)
  }
})

// 计算 Full GC 统计
const fullStats = computed(() => {
  if (!props.gcEvents || props.gcEvents.length === 0) {
    return getEmptyStats()
  }
  
  const fullEvents = props.gcEvents.filter(e => e.isFullGC)
  if (fullEvents.length === 0) {
    return getEmptyStats()
  }
  
  const times = fullEvents.map(e => e.pauseTime || 0)
  const totalTime = times.reduce((sum, t) => sum + t, 0)
  const totalReclaimed = fullEvents.reduce((sum, e) => {
    if (e.heapMemory && e.heapMemory.before && e.heapMemory.after) {
      return sum + (e.heapMemory.before - e.heapMemory.after)
    }
    return sum
  }, 0)
  
  const stats = calculateStats(times)
  
  return {
    count: fullEvents.length,
    totalReclaimed: formatBytes(totalReclaimed),
    totalTime: formatDuration(totalTime),
    avgTime: formatTime(stats.avg),
    stdDev: formatTime(stats.stdDev),
    minMax: `${formatTime(stats.min)} / ${formatTime(stats.max)}`,
    intervalAvg: calculateIntervalAvg(fullEvents)
  }
})

// 计算暂停统计
const pauseStats = computed(() => {
  if (!props.gcEvents || props.gcEvents.length === 0) {
    return {
      count: 0,
      totalTime: 'n/a',
      avgTime: 'n/a',
      stdDev: 'n/a',
      minMax: 'n/a'
    }
  }
  
  const events = props.gcEvents
  const totalTime = events.reduce((sum, e) => sum + (e.pauseTime || 0), 0)
  
  return {
    count: events.length,
    totalTime: formatDuration(totalTime),
    avgTime: formatTime(props.kpiMetrics?.latency?.avgPauseTime),
    stdDev: formatTime(props.kpiMetrics?.latency?.stdDevPauseTime),
    minMax: `${formatTime(props.kpiMetrics?.latency?.minPauseTime)} / ${formatTime(props.kpiMetrics?.latency?.maxPauseTime)}`
  }
})

function getEmptyStats() {
  return {
    count: 0,
    totalReclaimed: 'n/a',
    totalTime: 'n/a',
    avgTime: 'n/a',
    stdDev: 'n/a',
    minMax: 'n/a',
    intervalAvg: 'n/a'
  }
}

function calculateStats(values) {
  if (values.length === 0) {
    return { avg: null, stdDev: null, min: null, max: null }
  }
  
  const avg = values.reduce((sum, v) => sum + v, 0) / values.length
  const min = Math.min(...values)
  const max = Math.max(...values)
  
  const variance = values.reduce((sum, v) => sum + Math.pow(v - avg, 2), 0) / values.length
  const stdDev = Math.sqrt(variance)
  
  return { avg, stdDev, min, max }
}

function calculateIntervalAvg(events) {
  if (events.length < 2) return 'n/a'
  
  const intervals = []
  for (let i = 1; i < events.length; i++) {
    intervals.push(events[i].timestamp - events[i - 1].timestamp)
  }
  
  const avgInterval = intervals.reduce((sum, i) => sum + i, 0) / intervals.length
  return formatDuration(avgInterval)
}

function formatBytes(bytes) {
  if (!bytes || bytes === 0) return 'n/a'
  
  if (bytes >= 1024 * 1024 * 1024 * 1024) {
    return (bytes / (1024 * 1024 * 1024 * 1024)).toFixed(2) + ' tb'
  } else if (bytes >= 1024 * 1024 * 1024) {
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' gb'
  } else if (bytes >= 1024 * 1024) {
    return (bytes / (1024 * 1024)).toFixed(2) + ' mb'
  } else if (bytes >= 1024) {
    return (bytes / 1024).toFixed(2) + ' kb'
  }
  return bytes + ' b'
}

function formatTime(ms) {
  if (ms === null || ms === undefined) return 'n/a'
  return ms.toFixed(1) + ' ms'
}

function formatDuration(ms) {
  if (ms === null || ms === undefined) return 'n/a'
  
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  const remainingMs = Math.floor(ms % 1000)
  const remainingSec = seconds % 60
  const remainingMin = minutes % 60
  
  if (hours > 0) {
    return `${hours}h ${remainingMin}m ${remainingSec}s`
  } else if (minutes > 0) {
    return `${minutes} min ${remainingSec} sec ${remainingMs} ms`
  } else if (seconds > 0) {
    return `${seconds} sec ${remainingMs} ms`
  } else {
    return ms.toFixed(1) + ' ms'
  }
}
</script>

<style lang="scss" scoped>
.stats-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  height: 100%;
  
  .stats-card-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 16px 0;
    padding-bottom: 12px;
    border-bottom: 2px solid #e4e7ed;
  }
  
  .stats-table {
    .stats-row {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #f5f7fa;
      
      &:last-child {
        border-bottom: none;
      }
      
      .stats-label {
        font-size: 13px;
        color: #606266;
        background: #f5f7fa;
        padding: 4px 8px;
        border-radius: 4px;
        flex: 1;
        margin-right: 8px;
      }
      
      .stats-value {
        font-size: 13px;
        font-weight: 500;
        color: #303133;
        text-align: right;
        white-space: nowrap;
      }
    }
  }
}

@media (max-width: 768px) {
  .stats-card {
    margin-bottom: 16px;
  }
}
</style>

