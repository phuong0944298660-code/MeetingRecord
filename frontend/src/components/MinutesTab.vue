<template>
  <div class="minutes-tab" v-if="meeting">
    <!-- 会议概览头部 -->
    <div class="minutes-header" v-if="meeting.overview || meeting.summary">
      <div class="header-title">
        <svg viewBox="0 0 24 24" width="24" height="24">
          <path fill="currentColor" d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
        </svg>
        <span>会议纪要</span>
      </div>
      <p class="overview-text">{{ meeting.overview || meeting.summary }}</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: #ECF5FF; color: #409EFF;">
          <svg viewBox="0 0 24 24" width="24" height="24">
            <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2"/>
            <polyline points="12,6 12,12 16,14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ meeting.duration || '--' }}</div>
          <div class="stat-label">会议时长</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: #F0F9EB; color: #67C23A;">
          <svg viewBox="0 0 24 24" width="24" height="24">
            <path fill="currentColor" d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ meeting.stats?.totalSpeakers || meeting.speakers?.length || 0 }}人</div>
          <div class="stat-label">参与人数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: #FDF6EC; color: #E6A23C;">
          <svg viewBox="0 0 24 24" width="24" height="24">
            <path fill="currentColor" d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ totalTodos }}项</div>
          <div class="stat-label">待办事项</div>
        </div>
      </div>
    </div>

    <!-- 发言统计 -->
    <div class="content-card" v-if="meeting.stats?.speakerStats?.length > 0">
      <div class="card-header">
        <svg viewBox="0 0 24 24" width="20" height="20" style="color: #409EFF;">
          <path fill="currentColor" d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
        </svg>
        <span class="card-title">发言统计</span>
      </div>
      <div class="card-body">
        <div class="speaker-stats">
          <div v-for="stat in meeting.stats.speakerStats" :key="stat.name" class="speaker-stat-item">
            <div class="speaker-name">{{ stat.name }}</div>
            <div class="speaker-bar-container">
              <div class="speaker-bar" :style="{ width: stat.percentage + '%' }"></div>
            </div>
            <div class="speaker-time">{{ stat.speakTime }} ({{ stat.percentage }}%)</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 话题列表 -->
    <div class="topics-section" v-if="meeting.topics?.length > 0">
      <div class="section-header">
        <svg viewBox="0 0 24 24" width="24" height="24" style="color: #409EFF;">
          <path fill="currentColor" d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9h-4v4h-2v-4H9V9h4V5h2v4h4v2z"/>
        </svg>
        <span>讨论话题 ({{ meeting.topics.length }})</span>
      </div>

      <div class="topic-timeline">
        <div
          v-for="topic in meeting.topics"
          :key="topic.id"
          class="topic-card"
        >
          <!-- 话题头部 -->
          <div class="topic-header">
            <div class="topic-number">{{ topic.id }}</div>
            <div class="topic-title-wrapper">
              <h3 class="topic-title">{{ topic.title }}</h3>
              <span class="topic-time" v-if="topic.timeRange">
                <svg viewBox="0 0 24 24" width="14" height="14">
                  <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2"/>
                  <polyline points="12,6 12,12 16,14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
                {{ topic.timeRange }}
              </span>
            </div>
          </div>

          <!-- 话题内容 -->
          <div class="topic-body">
            <!-- 详细总结 -->
            <div class="topic-summary" v-if="topic.summary">
              {{ topic.summary }}
            </div>

            <!-- 关键要点 -->
            <div class="key-points" v-if="topic.keyPoints?.length > 0">
              <div class="sub-title">
                <svg viewBox="0 0 24 24" width="16" height="16" style="color: #67C23A;">
                  <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                </svg>
                关键要点
              </div>
              <ul class="points-list">
                <li v-for="(point, idx) in topic.keyPoints" :key="idx">{{ point }}</li>
              </ul>
            </div>

            <!-- 参与人 -->
            <div class="topic-speakers" v-if="topic.speakers?.length > 0">
              <div class="sub-title">
                <svg viewBox="0 0 24 24" width="16" height="16" style="color: #409EFF;">
                  <path fill="currentColor" d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
                </svg>
                参与讨论
              </div>
              <div class="speakers-tags">
                <span v-for="speaker in topic.speakers" :key="speaker" class="speaker-tag">{{ speaker }}</span>
              </div>
            </div>

            <!-- 话题待办 -->
            <div class="topic-todos" v-if="topic.todos?.length > 0">
              <div class="sub-title">
                <svg viewBox="0 0 24 24" width="16" height="16" style="color: #E6A23C;">
                  <path fill="currentColor" d="M19 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.11 0 2-.9 2-2V5c0-1.1-.89-2-2-2zm-9 14l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
                待办事项
              </div>
              <div class="todos-list">
                <div
                  v-for="todo in topic.todos"
                  :key="todo.id"
                  class="todo-item"
                  :class="{ completed: todo.done }"
                  @click.stop="toggleTodo(todo.id)"
                >
                  <div class="checkbox" :class="{ checked: todo.done }">
                    <svg v-if="todo.done" viewBox="0 0 24 24" width="12" height="12">
                      <polyline points="20,6 9,17 4,12" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </div>
                  <span class="todo-text">{{ todo.content }}</span>
                  <span v-if="todo.assignee" class="todo-assignee">@{{ todo.assignee }}</span>
                </div>
              </div>
            </div>

            <!-- 话题决策 -->
            <div class="topic-decisions" v-if="topic.decisions?.length > 0">
              <div class="sub-title">
                <svg viewBox="0 0 24 24" width="16" height="16" style="color: #F56C6C;">
                  <path fill="currentColor" d="M9 21c0 .5.4 1 1 1h4c.6 0 1-.5 1-1v-1H9v1zm3-19C8.1 2 5 5.1 5 9c0 2.4 1.2 4.5 3 5.7V17c0 .5.4 1 1 1h6c.6 0 1-.5 1-1v-2.3c1.8-1.3 3-3.4 3-5.7 0-3.9-3.1-7-7-7z"/>
                </svg>
                重要决策
              </div>
              <div class="decisions-list">
                <div v-for="(decision, idx) in topic.decisions" :key="idx" class="decision-item">
                  <span class="decision-number">{{ idx + 1 }}</span>
                  <span class="decision-text">{{ decision }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 兼容旧格式：全局待办事项 -->
    <div class="content-card" v-else-if="meeting.todos?.length > 0">
      <div class="card-header">
        <svg viewBox="0 0 24 24" width="20" height="20" style="color: #67C23A;">
          <path fill="currentColor" d="M19 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.11 0 2-.9 2-2V5c0-1.1-.89-2-2-2zm-9 14l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
        </svg>
        <span class="card-title">待办事项</span>
        <span class="card-subtitle">{{ completedTodos }}/{{ meeting.todos.length }} 已完成</span>
      </div>
      <div class="card-body">
        <div
          v-for="todo in meeting.todos"
          :key="todo.id"
          class="todo-item"
          :class="{ completed: todo.done }"
          @click="toggleTodo(todo.id)"
        >
          <div class="checkbox" :class="{ checked: todo.done }">
            <svg v-if="todo.done" viewBox="0 0 24 24" width="12" height="12">
              <polyline points="20,6 9,17 4,12" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <span class="todo-text">{{ todo.content }}</span>
          <span v-if="todo.assignee" class="todo-assignee">@{{ todo.assignee }}</span>
        </div>
      </div>
    </div>

    <!-- 兼容旧格式：全局决策 -->
    <div class="content-card" v-if="!meeting.topics && meeting.decisions?.length > 0">
      <div class="card-header">
        <svg viewBox="0 0 24 24" width="20" height="20" style="color: #E6A23C;">
          <path fill="currentColor" d="M9 21c0 .5.4 1 1 1h4c.6 0 1-.5 1-1v-1H9v1zm3-19C8.1 2 5 5.1 5 9c0 2.4 1.2 4.5 3 5.7V17c0 .5.4 1 1 1h6c.6 0 1-.5 1-1v-2.3c1.8-1.3 3-3.4 3-5.7 0-3.9-3.1-7-7-7z"/>
        </svg>
        <span class="card-title">敲定决策</span>
      </div>
      <div class="card-body">
        <div
          v-for="(decision, index) in meeting.decisions"
          :key="index"
          class="decision-item"
        >
          <span class="decision-number">{{ index + 1 }}</span>
          <span class="decision-text">{{ decision }}</span>
        </div>
      </div>
    </div>

    <!-- 处理中状态 -->
    <div v-if="!meeting.summary && !meeting.overview && meeting.status === 0" class="processing-content">
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

    <!-- 未选择会议 -->
    <div v-if="!meeting" class="empty-state">
      <svg viewBox="0 0 24 24" width="80" height="80" style="opacity: 0.2;">
        <path fill="#C0C4CC" d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6z"/>
        <path fill="#C0C4CC" d="M14 2v6h6"/>
      </svg>
      <p class="empty-title">选择一个会议查看详情</p>
      <p class="empty-desc">或上传新的会议录音</p>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onUnmounted, watch } from 'vue'
import { useMeetingStore } from '../stores/meeting.js'

const props = defineProps({
  meeting: {
    type: Object,
    default: null
  }
})

const meetingStore = useMeetingStore()

const completedTodos = computed(() => {
  return props.meeting?.todos?.filter(t => t.done).length || 0
})

const totalTodos = computed(() => {
  // 统计所有话题中的待办 + 全局待办
  let count = 0
  if (props.meeting?.topics) {
    props.meeting.topics.forEach(topic => {
      if (topic.todos) count += topic.todos.length
    })
  }
  if (props.meeting?.todos) {
    count += props.meeting.todos.length
  }
  return count
})

const toggleTodo = (todoId) => {
  meetingStore.toggleTodo(props.meeting.id, todoId)
}

// 进度条相关（处理中状态）
const progressPercent = ref(0)
const remainingTime = ref(4)
const processingStage = ref('正在分析音频文件...')
let progressTimer = null

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
    if (progressPercent.value < 95) {
      progressPercent.value += 0.4
      const currentStage = stages.slice().reverse().find(s => progressPercent.value >= s.percent)
      if (currentStage) {
        processingStage.value = currentStage.text
        remainingTime.value = currentStage.time
      }
    } else {
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

let pollTimer = null
const startPolling = () => {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    if (props.meeting?.id && props.meeting?.status === 0) {
      await meetingStore.refreshMeeting(props.meeting.id)
    } else if (props.meeting?.status === 1) {
      stopPolling()
    }
  }, 3000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

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
.minutes-tab {
  padding: 24px;
}

/* 会议概览头部 */
.minutes-header {
  background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  color: white;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}

.header-title svg {
  opacity: 0.9;
}

.overview-text {
  font-size: 14px;
  line-height: 1.8;
  opacity: 0.95;
  margin: 0;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #EBEEF5;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

/* 发言统计 */
.speaker-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.speaker-stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.speaker-name {
  width: 80px;
  font-size: 14px;
  color: #606266;
  flex-shrink: 0;
}

.speaker-bar-container {
  flex: 1;
  height: 8px;
  background: #EBEEF5;
  border-radius: 4px;
  overflow: hidden;
}

.speaker-bar {
  height: 100%;
  background: linear-gradient(90deg, #409EFF 0%, #67C23A 100%);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.speaker-time {
  width: 120px;
  font-size: 13px;
  color: #909399;
  text-align: right;
  flex-shrink: 0;
}

/* 话题区域 */
.topics-section {
  margin-top: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.topic-timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.topic-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #EBEEF5;
  overflow: hidden;
  transition: all 0.3s;
}

.topic-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.topic-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #FAFAFA;
  border-bottom: 1px solid #EBEEF5;
}

.topic-number {
  width: 32px;
  height: 32px;
  background: #409EFF;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.topic-title-wrapper {
  flex: 1;
  min-width: 0;
}

.topic-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.topic-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.topic-body {
  padding: 20px;
}

.topic-summary {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px dashed #EBEEF5;
}

/* 子标题 */
.sub-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

/* 关键要点 */
.key-points {
  margin-bottom: 16px;
}

.points-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.points-list li {
  position: relative;
  padding-left: 16px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.points-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 6px;
  height: 6px;
  background: #67C23A;
  border-radius: 50%;
}

/* 参与人 */
.topic-speakers {
  margin-bottom: 16px;
}

.speakers-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.speaker-tag {
  padding: 4px 12px;
  background: #ECF5FF;
  color: #409EFF;
  border-radius: 12px;
  font-size: 13px;
}

/* 话题待办和决策 */
.topic-todos, .topic-decisions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #EBEEF5;
}

.todos-list, .decisions-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 内容卡片（旧格式兼容） */
.content-card {
  background: white;
  border-radius: 8px;
  border: 1px solid #EBEEF5;
  margin-bottom: 16px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid #EBEEF5;
  background: #FAFAFA;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-subtitle {
  font-size: 13px;
  color: #909399;
  margin-left: auto;
}

.card-body {
  padding: 20px;
}

/* 待办事项 */
.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.todo-item:hover {
  background: #F5F7FA;
}

.todo-item.completed .todo-text {
  text-decoration: line-through;
  color: #C0C4CC;
}

.checkbox {
  width: 20px;
  height: 20px;
  border: 2px solid #DCDFE6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.checkbox.checked {
  background: #67C23A;
  border-color: #67C23A;
}

.todo-text {
  font-size: 14px;
  color: #606266;
  flex: 1;
}

.todo-assignee {
  font-size: 12px;
  color: #409EFF;
  background: #ECF5FF;
  padding: 2px 8px;
  border-radius: 10px;
}

/* 决策 */
.decision-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: #FDF6EC;
  border-radius: 6px;
  margin-bottom: 8px;
}

.decision-number {
  width: 24px;
  height: 24px;
  background: #E6A23C;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.decision-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  padding-top: 2px;
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

.empty-desc {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }

  .minutes-header {
    padding: 16px;
  }

  .topic-header {
    padding: 12px 16px;
  }

  .topic-body {
    padding: 16px;
  }

  .speaker-name {
    width: 60px;
  }

  .speaker-time {
    width: 100px;
    font-size: 12px;
  }
}
</style>
