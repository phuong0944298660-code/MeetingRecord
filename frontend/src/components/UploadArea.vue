<template>
  <div class="upload-area">
    <!-- 未上传状态 -->
    <div
      v-if="!isUploading && !isProcessing"
      class="upload-dropzone"
      :class="{ 'drag-over': isDragOver }"
      @dragenter.prevent="isDragOver = true"
      @dragleave.prevent="isDragOver = false"
      @dragover.prevent
      @drop.prevent="handleDrop"
      @click="triggerFileInput"
    >
      <input
        ref="fileInput"
        type="file"
        accept=".mp3,.wav,.m4a,.aac,.ogg"
        style="display: none"
        @change="handleFileChange"
      />

      <div class="upload-icon-wrapper">
        <svg viewBox="0 0 24 24" width="64" height="64" class="upload-icon">
          <path fill="#C0C4CC" d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/>
        </svg>
      </div>

      <h3 class="upload-title">点击或拖拽上传音频文件</h3>
      <p class="upload-hint">支持 MP3、WAV、M4A、AAC 格式，最大 500MB</p>

      <el-button type="primary" size="large" class="upload-btn">
        <svg viewBox="0 0 24 24" width="16" height="16" style="margin-right: 6px;">
          <path fill="currentColor" d="M9 16h6v-6h4l-7-7-7 7h4v6zm-4 2h14v2H5v-2z"/>
        </svg>
        选择文件
      </el-button>
    </div>

    <!-- 上传中状态 -->
    <div v-else-if="isUploading" class="upload-progress">
      <div class="progress-header">
        <svg viewBox="0 0 24 24" width="32" height="32">
          <path fill="#409EFF" d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6z"/>
          <path fill="white" d="M14 2v6h6"/>
        </svg>
        <span class="progress-title">正在上传...</span>
      </div>

      <el-progress
        :percentage="uploadProgress"
        :stroke-width="8"
        :show-text="true"
        status="success"
      />

      <p class="progress-hint">请保持网络畅通，不要关闭页面</p>
    </div>

    <!-- AI处理中状态 -->
    <div v-else-if="isProcessing" class="upload-progress">
      <div class="progress-header">
        <svg viewBox="0 0 24 24" width="32" height="32" class="rotating">
          <path fill="#409EFF" d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z" opacity="0.3"/>
          <path fill="#409EFF" d="M12 2v4a6 6 0 0 1 6 6h4a10 10 0 0 0-10-10z"/>
        </svg>
        <div class="progress-title-wrapper">
          <span class="progress-title">{{ processingStage || 'AI正在处理中...' }}</span>
          <span v-if="processingMessage" class="progress-message">{{ processingMessage }}</span>
        </div>
      </div>

      <el-progress
        :percentage="Math.round(processingProgress)"
        :stroke-width="8"
        :show-text="true"
        status="exception"
        :color="'#409EFF'"
      />

      <div class="processing-steps">
        <div class="step" :class="{ active: processingProgress >= 10 }">
          <span class="step-dot"/>
          <span class="step-text">语音识别</span>
        </div>
        <div class="step" :class="{ active: processingProgress >= 50 }">
          <span class="step-dot"/>
          <span class="step-text">生成纪要</span>
        </div>
        <div class="step" :class="{ active: processingProgress >= 100 }">
          <span class="step-dot"/>
          <span class="step-text">完成</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useMeetingStore } from '../stores/meeting.js'
import { ElMessage } from 'element-plus'

const meetingStore = useMeetingStore()
const { uploadProgress, processingProgress, processingStage, processingMessage, isUploading, isProcessing } = storeToRefs(meetingStore)

const fileInput = ref(null)
const isDragOver = ref(false)

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileChange = (event) => {
  const file = event.target.files?.[0]
  if (file) {
    processFile(file)
  }
}

const handleDrop = (event) => {
  isDragOver.value = false
  const file = event.dataTransfer.files?.[0]
  if (file) {
    processFile(file)
  }
}

const processFile = async (file) => {
  console.log('处理文件:', file.name, '类型:', file.type, '大小:', file.size)

  // 验证文件类型
  const allowedTypes = ['audio/mpeg', 'audio/wav', 'audio/x-wav', 'audio/mp4', 'audio/aac', 'audio/ogg', 'audio/webm']
  const allowedExts = ['.mp3', '.wav', '.m4a', '.aac', '.ogg', '.webm']
  const ext = '.' + file.name.split('.').pop().toLowerCase()

  if (!allowedTypes.includes(file.type) && !allowedExts.includes(ext)) {
    ElMessage.error('不支持的文件格式，请上传 MP3、WAV、M4A、AAC 或 OGG 格式')
    return
  }

  // 验证文件大小 (500MB)
  const maxSize = 500 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小超过限制，最大支持 500MB')
    return
  }

  // 开始上传
  try {
    await meetingStore.uploadFile(file, (meeting) => {
      ElMessage.success('会议处理完成！')
    })
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败: ' + (error.message || '请检查后端服务是否运行'))
  }
}
</script>

<style scoped>
.upload-area {
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.upload-dropzone {
  border: 2px dashed #DCDFE6;
  border-radius: 8px;
  padding: 60px 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-dropzone:hover,
.upload-dropzone.drag-over {
  border-color: #409EFF;
  background: #F5F7FA;
}

.upload-icon-wrapper {
  margin-bottom: 20px;
}

.upload-icon {
  opacity: 0.6;
}

.upload-title {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 13px;
  color: #909399;
  margin-bottom: 24px;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
}

/* 进度样式 */
.upload-progress {
  padding: 40px;
  text-align: center;
}

.progress-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 30px;
}

.progress-title-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress-title {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.progress-message {
  font-size: 13px;
  color: #909399;
}

.progress-hint {
  margin-top: 20px;
  font-size: 13px;
  color: #909399;
}

/* 处理步骤 */
.processing-steps {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-top: 30px;
}

.step {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0.4;
  transition: opacity 0.3s;
}

.step.active {
  opacity: 1;
}

.step-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #C0C4CC;
}

.step.active .step-dot {
  background: #409EFF;
}

.step-text {
  font-size: 13px;
  color: #606266;
}

/* 旋转动画 */
@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.rotating {
  animation: rotate 1s linear infinite;
}
</style>
