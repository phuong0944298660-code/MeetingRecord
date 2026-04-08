import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/**
 * WebSocket 客户端服务
 * 用于接收实时会议处理进度
 */
class WebSocketService {
  constructor() {
    this.client = null
    this.subscriptions = new Map()
    this.isConnected = false
    this.reconnectDelay = 5000
  }

  /**
   * 连接到 WebSocket 服务器
   */
  connect() {
    return new Promise((resolve, reject) => {
      try {
        // 构建 SockJS 连接（使用代理路径，避免跨域）
        const socket = new SockJS('/ws/meeting')

        this.client = new Client({
          webSocketFactory: () => socket,
          debug: (str) => {
            // 生产环境可以关闭
            console.log('[WebSocket]', str)
          },
          reconnectDelay: this.reconnectDelay,
          onConnect: () => {
            console.log('WebSocket 连接成功')
            this.isConnected = true
            resolve()
          },
          onDisconnect: () => {
            console.log('WebSocket 断开连接')
            this.isConnected = false
          },
          onStompError: (frame) => {
            console.error('WebSocket STOMP 错误:', frame)
            reject(frame)
          }
        })

        this.client.activate()
      } catch (error) {
        console.error('WebSocket 连接失败:', error)
        reject(error)
      }
    })
  }

  /**
   * 订阅会议进度
   * @param {number} meetingId 会议ID
   * @param {Function} onProgress 进度回调函数 (progress) => void
   */
  subscribeMeetingProgress(meetingId, onProgress) {
    if (!this.isConnected) {
      console.warn('WebSocket 未连接，无法订阅')
      return null
    }

    const destination = `/topic/meeting/${meetingId}/progress`

    const subscription = this.client.subscribe(destination, (message) => {
      try {
        const progress = JSON.parse(message.body)
        console.log('收到进度更新:', progress)
        onProgress(progress)
      } catch (error) {
        console.error('解析进度消息失败:', error)
      }
    })

    this.subscriptions.set(meetingId, subscription)
    console.log(`已订阅会议 ${meetingId} 的进度`)

    return subscription
  }

  /**
   * 取消订阅会议进度
   * @param {number} meetingId 会议ID
   */
  unsubscribeMeetingProgress(meetingId) {
    const subscription = this.subscriptions.get(meetingId)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(meetingId)
      console.log(`已取消订阅会议 ${meetingId} 的进度`)
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    // 取消所有订阅
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe()
    })
    this.subscriptions.clear()

    if (this.client) {
      this.client.deactivate()
      this.isConnected = false
      console.log('WebSocket 已断开')
    }
  }
}

// 导出单例实例
export const websocketService = new WebSocketService()
