<template>
  <div class="transcript-tab" v-if="meeting && meeting.speakers?.length > 0">
    <!-- 说话人筛选 -->
    <div class="speaker-filter">
      <span class="filter-label">说话人：</span>
      <div class="filter-tags">
        <span
          class="filter-tag"
          :class="{ active: selectedSpeakers.length === 0 }"
          @click="selectAllSpeakers"
        >
          全部
        </span>
        <span
          v-for="speaker in meeting.speakers"
          :key="speaker.id"
          class="filter-tag"
          :class="{ active: selectedSpeakers.includes(speaker.id) }"
          :style="getTagStyle(speaker)"
          @click="toggleSpeaker(speaker.id)"
        >
          {{ speaker.name }}
        </span>
      </div>
    </div>

    <!-- 逐字稿时间线 -->
    <div class="transcript-timeline">
      <div
        v-for="(item, index) in filteredTranscript"
        :key="index"
        class="timeline-item"
      >
        <div class="timeline-avatar" :style="{ background: item.speaker.color }">
          {{ item.speaker.name.charAt(item.speaker.name.length - 1) }}
        </div>
        <div class="timeline-content">
          <div class="timeline-header">
            <span class="speaker-name">{{ item.speaker.name }}</span>
            <span class="timestamp">{{ item.time }}</span>
          </div>
          <div class="timeline-text">
            {{ item.text }}
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 处理中 -->
  <div v-else-if="meeting && meeting.status === 0" class="processing-content">
    <div class="processing-animation">
      <div class="spinner"></div>
    </div>
    <p class="processing-title">AI正在处理中...</p>
    <p class="processing-status">{{ processingStage }}</p>

    <!-- 进度条 -->
    <div class="progress-container">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <div class="progress-info">
        <span class="progress-percent">{{ progressPercent.toFixed(1) }}%</span>
        <span class="progress-time">预计剩余 {{ remainingTime }} 分钟</span>
      </div>
    </div>

    <p class="processing-hint">处理过程中您可以关闭页面，后台将继续处理</p>
  </div>

  <!-- 无数据 -->
  <div v-else-if="meeting" class="empty-content">
    <svg viewBox="0 0 24 24" width="64" height="64" style="opacity: 0.3;">
      <path fill="#C0C4CC" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/>
    </svg>
    <p class="empty-text">暂无逐字稿数据</p>
  </div>

  <!-- 未选择 -->
  <div v-else class="empty-state">
    <svg viewBox="0 0 24 24" width="80" height="80" style="opacity: 0.2;">
      <path fill="#C0C4CC" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/>
    </svg>
    <p class="empty-title">选择一个会议查看逐字稿</p>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted, watch } from 'vue'
import { useMeetingStore } from '../stores/meeting.js'

const props = defineProps({
  meeting: {
    type: Object,
    default: null
  }
})

const meetingStore = useMeetingStore()
const selectedSpeakers = ref([])

// 将所有说话人的发言按时间排序
const allSegments = computed(() => {
  if (!props.meeting?.speakers) return []

  const segments = []
  props.meeting.speakers.forEach(speaker => {
    if (speaker.segments) {
      speaker.segments.forEach(segment => {
        segments.push({
          ...segment,
          speaker: speaker,
          speakerId: speaker.id
        })
      })
    }
  })

  // 按时间排序
  return segments.sort((a, b) => {
    return timeToSeconds(a.time) - timeToSeconds(b.time)
  })
})

const filteredTranscript = computed(() => {
  if (selectedSpeakers.value.length === 0) {
    return allSegments.value
  }
  return allSegments.value.filter(item =>
    selectedSpeakers.value.includes(item.speakerId)
  )
})

const timeToSeconds = (timeStr) => {
  const parts = timeStr.split(':').map(Number)
  return parts[0] * 3600 + parts[1] * 60 + parts[2]
}

const selectAllSpeakers = () => {
  selectedSpeakers.value = []
}

const toggleSpeaker = (speakerId) => {
  const index = selectedSpeakers.value.indexOf(speakerId)
  if (index > -1) {
    selectedSpeakers.value.splice(index, 1)
  } else {
    selectedSpeakers.value.push(speakerId)
  }
}

const getTagStyle = (speaker) => {
  const isSelected = selectedSpeakers.value.includes(speaker.id)
  if (isSelected) {
    return {
      background: speaker.color,
      color: 'white',
      borderColor: speaker.color
    }
  }
  return {
    background: 'white',
    color: speaker.color,
    borderColor: speaker.color
  }
}

// 进度条相关
const progressPercent = ref(0)
const remainingTime = ref(4)
const processingStage = ref('正在分析音频文件...')
let progressTimer = null

// 处理阶段文本
const stages = [
  { percent: 0, text: '正在分析音频文件...', time: 4 },
  { percent: 20, text: '正在识别说话人...', time: 3 },
  { percent: 40, text: '正在生成会议转录稿...', time: 2 },
  { percent: 60, text: '正在提取待办事项...', time: 2 },
  { percent: 80, text: '正在生成会议纪要...', time: 1 },
  { percent: 95, text: '正在保存结果...', time: 1 }
]

const startProgress = () => {
  progressPercent.value = 0
  progressTimer = setInterval(() => {
    // 每2秒增加1%，约4分钟达到95%
    if (progressPercent.value < 95) {
      progressPercent.value += 0.4

      // 更新阶段文本
      const currentStage = stages.slice().reverse().find(s => progressPercent.value >= s.percent)
      if (currentStage) {
        processingStage.value = currentStage.text
        remainingTime.value = currentStage.time
      }
    } else {
      // 达到95%后缓慢前进
      if (progressPercent.value < 99) {
        progressPercent.value += 0.1
      }
    }
  }, 2000)
}

const stopProgress = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

// 轮询检测后端状态
let pollTimer = null
const startPolling = () => {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    if (props.meeting?.id && props.meeting?.status === 0) {
      await meetingStore.refreshMeeting(props.meeting.id)
    } else if (props.meeting?.status === 1) {
      // 已完成，停止轮询
      stopPolling()
    }
  }, 3000) // 每3秒轮询一次
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 监听会议状态变化
watch(() => props.meeting?.status, (newStatus) => {
  if (newStatus === 0) {
    startProgress()
    startPolling()
  } else {
    stopProgress()
    stopPolling()
    if (newStatus === 1) {
      progressPercent.value = 100
      processingStage.value = '处理完成！'
    }
  }
}, { immediate: true })

// 监听会议ID变化（切换会议时）
watch(() => props.meeting?.id, (newId, oldId) => {
  if (newId !== oldId) {
    stopProgress()
    stopPolling()
    if (props.meeting?.status === 0) {
      startProgress()
      startPolling()
    }
  }
})

onUnmounted(() => {
  stopProgress()
  stopPolling()
})
</script>

<style scoped>
.transcript-tab {
  padding: 24px;
}

/* 说话人筛选 */
.speaker-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.filter-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tag {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #DCDFE6;
  background: white;
  color: #606266;
}

.filter-tag:hover {
  opacity: 0.8;
}

.filter-tag.active {
  font-weight: 500;
}

/* 时间线 */
.transcript-timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #EBEEF5;
  transition: all 0.2s;
}

.timeline-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.timeline-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.timeline-content {
  flex: 1;
  min-width: 0;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.speaker-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.timestamp {
  font-size: 12px;
  color: #C0C4CC;
  font-family: monospace;
}

.timeline-text {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
}

/* 处理中状态 */
.processing-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 40px;
  text-align: center;
  background: white;
  border-radius: 8px;
  border: 1px dashed #DCDFE6;
  margin: 24px;
}

.processing-animation {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #ECF5FF;
  border-top-color: #409EFF;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.processing-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.processing-status {
  font-size: 14px;
  color: #606266;
  margin-bottom: 24px;
}

.progress-container {
  width: 100%;
  max-width: 400px;
  margin-bottom: 16px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #EBEEF5;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #409EFF 0%, #67C23A 100%);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-percent {
  font-size: 16px;
  font-weight: 600;
  color: #409EFF;
}

.progress-time {
  font-size: 13px;
  color: #909399;
}

.processing-hint {
  font-size: 12px;
  color: #C0C4CC;
  margin-top: 8px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-title {
  font-size: 16px;
  color: #303133;
  margin-top: 16px;
  font-weight: 500;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  background: white;
  border-radius: 8px;
  border: 1px dashed #DCDFE6;
  margin: 24px;
}

.empty-text {
  font-size: 15px;
  color: #606266;
  margin-top: 16px;
}

.empty-subtext {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .timeline-item {
    padding: 12px;
  }

  .timeline-avatar {
    width: 32px;
    height: 32px;
    font-size: 12px;
  }
}
</style>
