<template>
  <aside class="sidebar">
    <!-- Logo区域 -->
    <div class="sidebar-header">
      <div class="logo">
        <svg class="logo-icon" viewBox="0 0 24 24" width="28" height="28">
          <path fill="#409EFF" d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
          <path fill="#409EFF" d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
        </svg>
        <span class="logo-text">会议纪要</span>
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="search-box">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索会议..."
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <svg class="search-icon" viewBox="0 0 24 24" width="16" height="16">
            <circle cx="11" cy="11" r="8" fill="none" stroke="#C0C4CC" stroke-width="2"/>
            <path d="m21 21-4.35-4.35" fill="none" stroke="#C0C4CC" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </template>
      </el-input>
    </div>

    <!-- 统计信息 -->
    <div class="stats-bar">
      <span class="stats-text">共 {{ meetingStore.meetingCount }} 条记录</span>
    </div>

    <!-- 会议列表 -->
    <div class="meeting-list" v-if="filteredMeetings.length > 0">
      <div
        v-for="meeting in filteredMeetings"
        :key="meeting.id"
        class="meeting-item"
        :class="{ active: currentMeeting?.id === meeting.id }"
        @click="selectMeeting(meeting)"
      >
        <div class="meeting-icon" :style="{ background: getStatusColor(meeting.status) }">
          <svg viewBox="0 0 24 24" width="18" height="18">
            <path fill="white" d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14,2 14,8 20,8" fill="none" stroke="white" stroke-width="2"/>
          </svg>
        </div>
        <div class="meeting-info">
          <div class="meeting-title">{{ meeting.title }}</div>
          <div class="meeting-meta">
            <span class="meeting-date">{{ formatDate(meeting.createdAt) }}</span>
            <span class="meeting-status" :class="getStatusClass(meeting.status)">
              {{ getStatusText(meeting.status) }}
            </span>
          </div>
        </div>
        <el-dropdown trigger="click" @command="handleCommand($event, meeting)">
          <span class="more-btn" @click.stop>
            <el-icon><MoreFilled /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="delete" :icon="Delete">删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" style="opacity: 0.3;">
        <path fill="#C0C4CC" d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
        <polyline points="14,2 14,8 20,8" fill="none" stroke="#C0C4CC" stroke-width="2"/>
      </svg>
      <p class="empty-text">{{ searchKeyword ? '未找到相关会议' : '暂无会议记录' }}</p>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useMeetingStore } from '../stores/meeting.js'
import { Delete, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const meetingStore = useMeetingStore()
const { currentMeeting, searchKeyword, filteredMeetings } = storeToRefs(meetingStore)

const handleSearch = () => {
  meetingStore.setSearchKeyword(searchKeyword.value)
}

const selectMeeting = (meeting) => {
  meetingStore.selectMeeting(meeting)
}

const getStatusColor = (status) => {
  const colors = {
    0: '#E6A23C', // 处理中 - 橙色
    1: '#409EFF', // 完成 - 蓝色
    2: '#F56C6C'  // 失败 - 红色
  }
  return colors[status] || '#909399'
}

const getStatusText = (status) => {
  const texts = {
    0: '处理中',
    1: '已完成',
    2: '失败'
  }
  return texts[status] || '未知'
}

const getStatusClass = (status) => {
  const classes = {
    0: 'status-processing',
    1: 'status-success',
    2: 'status-error'
  }
  return classes[status] || ''
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()

  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const handleCommand = async (command, meeting) => {
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm(
        `确定要删除会议 "${meeting.title}" 吗？`,
        '确认删除',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      meetingStore.deleteMeeting(meeting.id)
      ElMessage.success('删除成功')
    } catch {
      // 用户取消
    }
  }
}
</script>

<style scoped>
.sidebar {
  width: 260px;
  height: 100vh;
  background: #fff;
  border-right: 1px solid #EBEEF5;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #EBEEF5;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  flex-shrink: 0;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.search-box {
  padding: 16px;
}

.search-icon {
  display: block;
}

.stats-bar {
  padding: 0 16px 8px;
  font-size: 12px;
  color: #909399;
}

.meeting-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}

.meeting-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.meeting-item:hover {
  background: #F5F7FA;
}

.meeting-item.active {
  background: #ECF5FF;
}

.meeting-item.active .meeting-title {
  color: #409EFF;
  font-weight: 500;
}

.meeting-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 12px;
}

.meeting-info {
  flex: 1;
  min-width: 0;
}

.meeting-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meeting-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.meeting-date {
  color: #909399;
}

.meeting-status {
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.status-processing {
  background: #FDF6EC;
  color: #E6A23C;
}

.status-success {
  background: #F0F9EB;
  color: #67C23A;
}

.status-error {
  background: #FEF0F0;
  color: #F56C6C;
}

.more-btn {
  padding: 4px;
  color: #C0C4CC;
  opacity: 0;
  transition: opacity 0.2s;
}

.meeting-item:hover .more-btn {
  opacity: 1;
}

.more-btn:hover {
  color: #409EFF;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.empty-text {
  margin-top: 12px;
  font-size: 13px;
  color: #C0C4CC;
}

/* 滚动条样式 */
.meeting-list::-webkit-scrollbar {
  width: 4px;
}

.meeting-list::-webkit-scrollbar-track {
  background: transparent;
}

.meeting-list::-webkit-scrollbar-thumb {
  background: #DCDFE6;
  border-radius: 2px;
}

.meeting-list::-webkit-scrollbar-thumb:hover {
  background: #C0C4CC;
}
</style>
