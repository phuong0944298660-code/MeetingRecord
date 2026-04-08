<template>
  <div class="app">
    <!-- 侧边栏 -->
    <Sidebar />

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 未选择会议时显示上传 -->
      <div v-if="!currentMeeting && !isUploading && !isProcessing" class="content-wrapper">
        <div class="page-header">
          <h1 class="page-title">上传会议录音</h1>
          <p class="page-desc">支持智能语音识别、说话人分离、自动生成会议纪要</p>
        </div>
        <UploadArea />
      </div>

      <!-- 会议详情 -->
      <div v-else-if="currentMeeting" class="content-wrapper">
        <div class="meeting-header">
          <div class="meeting-title-wrapper">
            <h1 class="meeting-title">{{ currentMeeting.title }}</h1>
            <span class="meeting-date">{{ currentMeeting.createdAt }}</span>
          </div>
          <el-button type="primary" @click="showUpload = true">
            <el-icon><Plus /></el-icon>
            新上传
          </el-button>
        </div>

        <!-- Tab切换 -->
        <div class="tabs-wrapper">
          <div class="tabs">
            <div
              class="tab"
              :class="{ active: activeTab === 'minutes' }"
              @click="activeTab = 'minutes'"
            >
              <el-icon><Document /></el-icon>
              会议纪要
            </div>
            <div
              class="tab"
              :class="{ active: activeTab === 'transcript' }"
              @click="activeTab = 'transcript'"
            >
              <el-icon><ChatDotRound /></el-icon>
              逐字稿
            </div>
          </div>
        </div>

        <!-- Tab内容 -->
        <div class="tab-content">
          <MinutesTab
            v-show="activeTab === 'minutes'"
            :meeting="currentMeeting"
          />
          <TranscriptTab
            v-show="activeTab === 'transcript'"
            :meeting="currentMeeting"
          />
        </div>
      </div>

      <!-- 上传/处理中 -->
      <div v-else class="content-wrapper">
        <div class="page-header">
          <h1 class="page-title">文件处理</h1>
        </div>
        <UploadArea />
      </div>
    </main>

    <!-- 新上传对话框 -->
    <el-dialog
      v-model="showUpload"
      title="上传新会议"
      width="600px"
      destroy-on-close
    >
      <UploadArea />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useMeetingStore } from './stores/meeting.js'
import Sidebar from './components/Sidebar.vue'
import UploadArea from './components/UploadArea.vue'
import MinutesTab from './components/MinutesTab.vue'
import TranscriptTab from './components/TranscriptTab.vue'
import { Plus, Document, ChatDotRound } from '@element-plus/icons-vue'

const meetingStore = useMeetingStore()
const { currentMeeting, isUploading, isProcessing } = storeToRefs(meetingStore)

const activeTab = ref('minutes')
const showUpload = ref(false)

// 初始化加载数据
onMounted(() => {
  meetingStore.init()
})

// 监听上传完成，关闭弹窗
watch(currentMeeting, (newVal) => {
  // 如果新选择了会议（从null变为有值，或切换到新会议），且弹窗打开，则关闭弹窗
  if (newVal && showUpload.value) {
    showUpload.value = false
  }
})
</script>

<style>
.app {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.main-content {
  flex: 1;
  overflow: hidden;
  background: #F5F7FA;
}

.content-wrapper {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

/* 页面头部 */
.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.page-desc {
  font-size: 14px;
  color: #909399;
}

/* 会议头部 */
.meeting-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #EBEEF5;
}

.meeting-title-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meeting-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.meeting-date {
  font-size: 13px;
  color: #909399;
}

/* Tab样式 */
.tabs-wrapper {
  background: white;
  border-radius: 8px 8px 0 0;
  border: 1px solid #EBEEF5;
  border-bottom: none;
  padding: 0 24px;
}

.tabs {
  display: flex;
  gap: 32px;
}

.tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 16px 8px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.2s;
}

.tab:hover {
  color: #409EFF;
}

.tab.active {
  color: #409EFF;
  border-bottom-color: #409EFF;
  font-weight: 500;
}

/* Tab内容 */
.tab-content {
  background: white;
  border-radius: 0 0 8px 8px;
  border: 1px solid #EBEEF5;
  border-top: none;
  min-height: 400px;
}

/* 滚动条 */
.content-wrapper::-webkit-scrollbar {
  width: 6px;
}

.content-wrapper::-webkit-scrollbar-track {
  background: transparent;
}

.content-wrapper::-webkit-scrollbar-thumb {
  background: #DCDFE6;
  border-radius: 3px;
}

.content-wrapper::-webkit-scrollbar-thumb:hover {
  background: #C0C4CC;
}

/* 对话框 */
:deep(.el-dialog__body) {
  padding: 0;
}

:deep(.upload-area) {
  box-shadow: none;
}

@media (max-width: 768px) {
  .app {
    flex-direction: column;
  }

  .main-content {
    height: calc(100vh - 60px);
  }

  .meeting-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
