const API_BASE = '/api'

// 统一请求处理
async function request(url, options = {}) {
  const token = localStorage.getItem('token')
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers
  })

  const data = await response.json()

  if (!response.ok || data.code !== 200) {
    throw new Error(data.message || '请求失败')
  }

  return data.data
}

// 会议相关API
export const meetingApi = {
  // 获取会议列表
  getList(keyword = '') {
    const url = keyword ? `/meeting/list?keyword=${encodeURIComponent(keyword)}` : '/meeting/list'
    return request(url)
  },

  // 获取会议详情
  getDetail(id) {
    return request(`/meeting/${id}`)
  },

  // 上传会议录音
  upload(file, onProgress) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      const formData = new FormData()
      formData.append('file', file)

      const token = localStorage.getItem('token')

      console.log('开始上传请求:', `${API_BASE}/meeting/upload`)

      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable && onProgress) {
          const percent = Math.round((e.loaded * 100) / e.total)
          onProgress(percent)
        }
      })

      xhr.addEventListener('load', () => {
        console.log('上传响应状态:', xhr.status)
        console.log('上传响应内容:', xhr.responseText)

        if (xhr.status === 200) {
          try {
            const data = JSON.parse(xhr.responseText)
            if (data.code === 200) {
              resolve(data.data)
            } else {
              reject(new Error(data.message || '上传失败'))
            }
          } catch (e) {
            reject(new Error('解析响应失败: ' + xhr.responseText))
          }
        } else {
          reject(new Error('上传失败: HTTP ' + xhr.status))
        }
      })

      xhr.addEventListener('error', (e) => {
        console.error('上传网络错误:', e)
        reject(new Error('网络错误，请检查后端服务是否运行'))
      })

      xhr.addEventListener('abort', () => {
        reject(new Error('上传已取消'))
      })

      xhr.open('POST', `${API_BASE}/meeting/upload`)
      if (token) {
        xhr.setRequestHeader('Authorization', `Bearer ${token}`)
      }
      xhr.send(formData)
    })
  },

  // 切换待办状态
  toggleTodo(meetingId, todoId) {
    return request(`/meeting/${meetingId}/todo/${todoId}/toggle`, {
      method: 'POST'
    })
  },

  // 删除会议
  delete(id) {
    return request(`/meeting/${id}`, {
      method: 'DELETE'
    })
  }
}

// 认证相关API
export const authApi = {
  // 登录
  login(username, password) {
    return request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    })
  },

  // 注册
  register(username, password, email) {
    return request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, password, email })
    })
  }
}
