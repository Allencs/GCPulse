<template>
  <div v-if="consecutiveFullGCInfo && consecutiveFullGCInfo.hasConsecutiveFullGC" 
       class="analysis-card slide-in-up consecutive-fullgc-warning">
    <div class="card-title">
      <el-icon :color="getSeverityColor()"><Warning /></el-icon>
      连续 Full GC 警告
      <el-tag :type="getSeverityType()" size="small" style="margin-left: 10px">
        {{ consecutiveFullGCInfo.severity }}
      </el-tag>
    </div>
    
    <el-alert
      :title="`检测到最多 ${consecutiveFullGCInfo.maxConsecutiveCount} 次连续 Full GC`"
      :type="getSeverityAlertType()"
      :closable="false"
      show-icon
    >
      <p><strong>严重程度：</strong>{{ consecutiveFullGCInfo.severity }}</p>
      <p><strong>序列数：</strong>{{ consecutiveFullGCInfo.sequences?.length || 0 }} 个</p>
      <p style="margin-top: 10px;">
        <strong>建议：</strong>立即检查应用程序是否存在内存泄漏，考虑增加堆内存大小，或优化对象生命周期管理。
      </p>
    </el-alert>
    
    <div v-if="consecutiveFullGCInfo.sequences && consecutiveFullGCInfo.sequences.length > 0" 
         style="margin-top: 20px">
      <h4 style="margin-bottom: 10px;">连续 Full GC 序列详情</h4>
      <el-table :data="consecutiveFullGCInfo.sequences" stripe>
        <el-table-column prop="count" label="连续次数" width="100" align="center" />
        <el-table-column label="开始时间" width="150" align="center">
          <template #default="scope">
            {{ formatTimestamp(scope.row.startTimestamp) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="150" align="center">
          <template #default="scope">
            {{ formatTimestamp(scope.row.endTimestamp) }}
          </template>
        </el-table-column>
        <el-table-column label="总持续时间" width="150" align="center">
          <template #default="scope">
            {{ formatDuration(scope.row.totalDuration) }}
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button 
              size="small" 
              @click="showSequenceDetails(scope.row)"
            >
              查看事件
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <!-- 事件详情对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      title="Full GC 事件详情" 
      width="80%"
    >
      <el-table :data="currentSequenceEvents" stripe>
        <el-table-column prop="eventType" label="事件类型" min-width="200" />
        <el-table-column label="时间戳" width="150">
          <template #default="scope">
            {{ formatTimestamp(scope.row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column label="暂停时间" width="120">
          <template #default="scope">
            {{ scope.row.pauseTime.toFixed(3) }} ms
          </template>
        </el-table-column>
        <el-table-column label="堆内存" min-width="200">
          <template #default="scope">
            <span v-if="scope.row.heapMemory">
              {{ formatMemory(scope.row.heapMemory.before) }} → {{ formatMemory(scope.row.heapMemory.after) }}
            </span>
            <span v-else>N/A</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Warning } from '@element-plus/icons-vue'

defineProps({
  consecutiveFullGCInfo: {
    type: Object,
    default: () => null
  }
})

const dialogVisible = ref(false)
const currentSequenceEvents = ref([])

function getSeverityColor() {
  const info = props.consecutiveFullGCInfo
  if (!info) return '#909399'
  
  switch(info.severity) {
    case 'CRITICAL': return '#F56C6C'
    case 'HIGH': return '#E6A23C'
    case 'MEDIUM': return '#E6A23C'
    default: return '#909399'
  }
}

function getSeverityType() {
  const info = props.consecutiveFullGCInfo
  if (!info) return 'info'
  
  switch(info.severity) {
    case 'CRITICAL': return 'danger'
    case 'HIGH': return 'warning'
    case 'MEDIUM': return 'warning'
    default: return 'info'
  }
}

function getSeverityAlertType() {
  const info = props.consecutiveFullGCInfo
  if (!info) return 'info'
  
  switch(info.severity) {
    case 'CRITICAL': return 'error'
    case 'HIGH': return 'warning'
    case 'MEDIUM': return 'warning'
    default: return 'info'
  }
}

function formatTimestamp(ts) {
  const seconds = Math.floor(ts / 1000)
  return `${seconds}s`
}

function formatDuration(ms) {
  if (ms >= 1000) {
    return (ms / 1000).toFixed(3) + ' s'
  }
  return ms.toFixed(3) + ' ms'
}

function formatMemory(bytes) {
  const mb = bytes / (1024 * 1024)
  return mb.toFixed(2) + ' MB'
}

function showSequenceDetails(sequence) {
  currentSequenceEvents.value = sequence.events || []
  dialogVisible.value = true
}
</script>

<style lang="scss" scoped>
.consecutive-fullgc-warning {
  border-left: 4px solid #F56C6C;
  
  h4 {
    color: #303133;
    font-size: 14px;
    font-weight: 500;
  }
}
</style>

