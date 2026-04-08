import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { meetingApi } from '../api/meeting.js'
import { websocketService } from '../api/websocket.js'

export const useMeetingStore = defineStore('meeting', () => {
  // State
  const meetings = ref([])
  const currentMeeting = ref(null)
  const searchKeyword = ref('')
  const uploadProgress = ref(0)
  const processingProgress = ref(0)
  const processingStage = ref('') // 当前阶段名称
  const processingMessage = ref('') // 当前状态消息
  const isUploading = ref(false)
  const isProcessing = ref(false)
  const loading = ref(false)

  // Getters
  const filteredMeetings = computed(() => {
    if (!searchKeyword.value) return meetings.value
    const keyword = searchKeyword.value.toLowerCase()
    return meetings.value.filter(meeting =>
      meeting.title?.toLowerCase().includes(keyword) ||
      meeting.summary?.toLowerCase().includes(keyword)
    )
  })

  const meetingCount = computed(() => meetings.value.length)

  // Actions
  // 获取会议列表
  async function fetchMeetings() {
    loading.value = true
    try {
      const data = await meetingApi.getList(searchKeyword.value)
      meetings.value = data || []
    } catch (error) {
      console.error('获取会议列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 选择会议
  async function selectMeeting(meeting) {
    if (!meeting) {
      currentMeeting.value = null
      return
    }

    try {
      const detail = await meetingApi.getDetail(meeting.id)
      currentMeeting.value = detail
    } catch (error) {
      console.error('获取会议详情失败:', error)
      currentMeeting.value = meeting
    }
  }

  function setSearchKeyword(keyword) {
    searchKeyword.value = keyword
  }

  function addMeeting(meeting) {
    meetings.value.unshift(meeting)
    currentMeeting.value = meeting
  }

  // 上传文件
  async function uploadFile(file, onComplete) {
    isUploading.value = true
    uploadProgress.value = 0
    isProcessing.value = false
    processingProgress.value = 0
    processingStage.value = ''
    processingMessage.value = ''

    try {
      console.log('开始上传文件:', file.name, '大小:', file.size)
      const data = await meetingApi.upload(file, (progress) => {
        uploadProgress.value = progress
        console.log('上传进度:', progress)
      })

      console.log('上传成功, meetingId:', data.meetingId)
      isUploading.value = false

      // 创建新会议记录
      const newMeeting = {
        id: data.meetingId,
        title: file.name.replace(/\.[^/.]+$/, ''),
        duration: '计算中...',
        fileSize: file.size,
        summary: '',
        todos: [],
        decisions: [],
        speakers: [],
        status: 0, // 处理中
        createdAt: new Date().toLocaleString('zh-CN')
      }

      addMeeting(newMeeting)

      // 开始 WebSocket 监听处理进度
      startProcessing(newMeeting.id, onComplete)

      return data
    } catch (error) {
      console.error('上传失败:', error)
      isUploading.value = false
      isProcessing.value = false
      throw error
    }
  }

  // 通过 WebSocket 监听 AI 处理进度
  async function startProcessing(meetingId, onComplete) {
    isProcessing.value = true
    processingProgress.value = 0
    processingStage.value = '准备处理'
    processingMessage.value = '正在连接服务器...'

    // 确保 WebSocket 已连接
    if (!websocketService.isConnected) {
      try {
        await websocketService.connect()
      } catch (error) {
        console.error('WebSocket 连接失败，将使用轮询方式获取进度:', error)
        processingMessage.value = '使用轮询方式获取进度...'
        // 如果 WebSocket 连接失败，使用轮询作为备选
        startPollingProgress(meetingId, onComplete)
        return
      }
    }

    // 订阅进度
    websocketService.subscribeMeetingProgress(meetingId, (progress) => {
      processingStage.value = progress.stageName || progress.stage
      processingProgress.value = progress.totalProgress || progress.progress
      processingMessage.value = progress.message

      console.log(`[${progress.stage}] 进度: ${progress.totalProgress}% - ${progress.message}`)

      // 处理完成或失败
      if (progress.stage === 'completed') {
        isProcessing.value = false
        websocketService.unsubscribeMeetingProgress(meetingId)
        // 刷新会议详情
        refreshMeeting(meetingId).then(() => {
          if (onComplete) onComplete(currentMeeting.value)
        })
      } else if (progress.stage === 'error') {
        isProcessing.value = false
        processingMessage.value = `处理失败: ${progress.error || progress.message}`
        websocketService.unsubscribeMeetingProgress(meetingId)
      }
    })
  }

  // 轮询进度（WebSocket 失败时的备选方案）
  async function startPollingProgress(meetingId, onComplete) {
    const interval = setInterval(async () => {
      try {
        const detail = await meetingApi.getDetail(meetingId)

        // 根据状态更新进度
        if (detail.status === 1) {
          // 已完成
          clearInterval(interval)
          processingProgress.value = 100
          processingStage.value = '处理完成'
          processingMessage.value = '会议处理完成'
          isProcessing.value = false

          // 更新会议详情
          const index = meetings.value.findIndex(m => m.id === meetingId)
          if (index > -1) {
            meetings.value[index] = detail
          }
          if (currentMeeting.value?.id === meetingId) {
            currentMeeting.value = detail
          }

          if (onComplete) onComplete(detail)
        } else if (detail.status === 2) {
          // 失败
          clearInterval(interval)
          processingStage.value = '处理失败'
          processingMessage.value = '会议处理失败，请重试'
          isProcessing.value = false
        } else {
          // 处理中 - 模拟缓慢增长
          if (processingProgress.value < 90) {
            processingProgress.value += Math.random() * 5
          }
          processingStage.value = detail.stage || '处理中'
          processingMessage.value = detail.message || '正在处理会议内容...'
        }
      } catch (error) {
        console.error('轮询进度失败:', error)
      }
    }, 2000)
  }

  // 刷新会议详情
  async function refreshMeeting(meetingId) {
    try {
      const detail = await meetingApi.getDetail(meetingId)
      const index = meetings.value.findIndex(m => m.id === meetingId)
      if (index > -1) {
        meetings.value[index] = detail
      }
      if (currentMeeting.value?.id === meetingId) {
        currentMeeting.value = detail
      }
    } catch (error) {
      console.error('刷新会议详情失败:', error)
    }
  }

  // 切换待办状态
  async function toggleTodo(meetingId, todoId) {
    try {
      await meetingApi.toggleTodo(meetingId, todoId)

      // 更新本地数据
      if (currentMeeting.value?.id === meetingId && currentMeeting.value.todos) {
        const todo = currentMeeting.value.todos.find(t => t.id === todoId)
        if (todo) {
          todo.done = !todo.done
        }
      }
    } catch (error) {
      console.error('切换待办状态失败:', error)
    }
  }

  // 删除会议
  async function deleteMeeting(id) {
    try {
      await meetingApi.delete(id)
      const index = meetings.value.findIndex(m => m.id === id)
      if (index > -1) {
        meetings.value.splice(index, 1)
      }
      if (currentMeeting.value?.id === id) {
        currentMeeting.value = meetings.value[0] || null
      }
    } catch (error) {
      console.error('删除会议失败:', error)
      throw error
    }
  }

  // 初始化
  async function init() {
    await fetchMeetings()
  }

  return {
    meetings,
    currentMeeting,
    searchKeyword,
    uploadProgress,
    processingProgress,
    processingStage,
    processingMessage,
    isUploading,
    isProcessing,
    loading,
    filteredMeetings,
    meetingCount,
    fetchMeetings,
    selectMeeting,
    setSearchKeyword,
    addMeeting,
    uploadFile,
    toggleTodo,
    deleteMeeting,
    refreshMeeting,
    init
  }
})
