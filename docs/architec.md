# 会议纪要应用 - 技术架构文档

> 本文档记录项目的技术架构选型、组件关系和数据流转。
> **更新记录**：每次技术架构变更时，需同步更新本文档。

---

## 一、整体架构

### 1.1 架构概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              用户层 (Client)                                  │
│                    Vue 3 + Element Plus + Pinia + Axios                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼ HTTPS
┌─────────────────────────────────────────────────────────────────────────────┐
│                              网关层 (Nginx)                                   │
│                     反向代理 / 负载均衡 / SSL终止 / 静态资源                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              应用层 (Backend)                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │   用户服务    │  │   会议服务    │  │   文件服务    │  │  AI处理服务       │ │
│  │  UserService │  │MeetingService│  │  FileService │  │ Whisper + GLM-5  │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────────┘ │
│                         Spring Boot 3.1.12 + MyBatis Plus                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              数据层 (Data)                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │    MySQL     │  │    Redis     │  │   本地存储    │  │   Python 脚本     │ │
│  │   业务数据    │  │  缓存/会话    │  │   音频文件    │  │ Whisper 转录     │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心流程

```
用户上传音频
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ 1. 文件上传                                                      │
│    - 前端: XMLHttpRequest 上传进度监控                             │
│    - 后端: Spring Boot MultipartFile 接收                          │
│    - 存储: 本地文件系统 (${user.home}/meeting-record/uploads)       │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. 语音识别 (Whisper)                                            │
│    - Java ProcessBuilder 调用 Python 脚本                          │
│    - OpenAI Whisper base 模型本地推理                              │
│    - 输出: 带时间戳的转录文本 (JSON)                                │
│    - 依赖: FFmpeg (音频解码)                                       │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. 纪要生成 (GLM-5)                                              │
│    - 转录文本 → GLM-5 API                                          │
│    - 生成: 摘要/待办/决策/说话人分离                                 │
│    - 输出: 结构化 JSON 会议纪要                                     │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. 数据持久化                                                    │
│    - MySQL: 会议元数据、转录结果、纪要内容                           │
│    - 文件系统: 原始音频文件保留                                      │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. 前端展示                                                      │
│    - 轮询检测处理状态 (3秒间隔)                                     │
│    - 双 Tab 展示: 会议纪要 + 逐字稿                                 │
│    - 实时进度条显示                                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、技术栈详情

### 2.1 前端技术栈

| 技术 | 版本 | 用途 | 备注 |
|------|------|------|------|
| Vue | 3.4.x | 前端框架 | Composition API |
| Element Plus | 2.x | UI 组件库 | 主题色 #409EFF |
| Pinia | 2.x | 状态管理 | 替代 Vuex |
| Axios | 1.x | HTTP 请求 | 封装 API 模块 |
| Vite | 5.x | 构建工具 | 开发服务器 |

**关键组件：**
- `UploadArea.vue` - 文件上传（拖拽 + 进度）
- `MinutesTab.vue` - 会议纪要展示
- `TranscriptTab.vue` - 逐字稿展示（说话人筛选）
- `Sidebar.vue` - 历史记录列表

### 2.2 后端技术栈

| 技术 | 版本 | 用途 | 备注 |
|------|------|------|------|
| Spring Boot | 3.1.12 | 应用框架 | 3.2.x 与 MyBatis Plus 不兼容 |
| MyBatis Plus | 3.5.6 | ORM 框架 | 简化 CRUD |
| JJWT | 0.12.x | JWT 认证 | parser() 替代 parserBuilder() |
| Fastjson2 | 2.x | JSON 处理 | 阿里巴巴 |
| Lombok | 1.x | 代码简化 | @Data @Slf4j |

**关键服务：**
- `MeetingService` - 会议核心业务逻辑
- `WhisperService` - 调用 Python Whisper 语音识别
- `KimiService` - 调用 GLM-5 API 生成纪要
- `MeetingController` - RESTful API 接口

### 2.3 数据存储

| 存储 | 版本 | 用途 | 配置 |
|------|------|------|------|
| MySQL | 8.0 | 业务数据 | allowPublicKeyRetrieval=true |
| Redis | 7.x | 缓存/会话 | 可选，当前版本未启用 |
| 本地文件 | - | 音频文件 | ${user.home}/meeting-record/uploads |

**数据库表：**
- `user` - 用户信息
- `meeting` - 会议记录（JSON 字段存储转录和纪要）

### 2.4 AI 服务

| 服务 | 类型 | 部署方式 | 配置 |
|------|------|----------|------|
| OpenAI Whisper | 语音识别 | 本地部署 | base 模型，CPU 推理 |
| GLM-5 | 文本生成 | 在线 API | https://apie.zhisuaninfo.com/v1 |

**Whisper 配置：**
```yaml
whisper:
  python-path: ${PYTHON_PATH:python}  # 从环境变量读取
  script-path: ${WHISPER_SCRIPT_PATH:./backend/whisper/transcribe.py}
  model: base  # tiny/base/small/medium/large
```

**模型缓存路径配置：**
```python
# backend/whisper/transcribe.py 中从环境变量读取
whisper_cache = os.environ.get("WHISPER_CACHE_DIR", "")
hf_home = os.environ.get("HF_HOME", "")
# 如果未设置，使用系统默认路径
```

**GLM-5 配置：**
```yaml
meeting:
  kimi:
    api-key: ${KIMI_API_KEY}  # 从环境变量读取
    base-url: ${KIMI_BASE_URL:https://apie.zhisuaninfo.com/v1}
    model: ${KIMI_MODEL:GLM-5}
```

---

## 三、模块关系

### 3.1 模块依赖图

```
frontend/
├── src/
│   ├── api/
│   │   └── meeting.js          → 调用后端 API
│   ├── stores/
│   │   └── meeting.js          → Pinia 状态管理
│   ├── components/
│   │   ├── Sidebar.vue         → 历史记录
│   │   ├── UploadArea.vue      → 文件上传
│   │   ├── MinutesTab.vue      → 会议纪要
│   │   └── TranscriptTab.vue   → 逐字稿
│   └── App.vue                 → 主布局
│
backend/
├── src/main/java/com/meeting/
│   ├── controller/
│   │   └── MeetingController.java  → REST API
│   ├── service/
│   │   ├── MeetingService.java     → 业务逻辑
│   │   ├── WhisperService.java     → 语音识别
│   │   └── KimiService.java        → 纪要生成
│   ├── mapper/
│   │   └── MeetingMapper.java      → 数据访问
│   └── entity/
│       └── Meeting.java            → 实体类
│
└── whisper/
    ├── transcribe.py           → Python 转录脚本
    └── requirements.txt        → Python 依赖
```

### 3.2 核心类关系

```
MeetingController
    │
    ├──→ MeetingService
    │       │
    │       ├──→ MeetingMapper (MySQL)
    │       │
    │       ├──→ WhisperService
    │       │       └──→ ProcessBuilder
    │       │               └──→ transcribe.py (Whisper)
    │       │
    │       └──→ KimiService
    │               └──→ RestTemplate
    │                       └──→ GLM-5 API
    │
    └──→ MeetingVO (DTO)
```

---

## 四、数据流转

### 4.1 音频处理数据流

```
[浏览器]          [Spring Boot]              [Python]              [GLM-5 API]
   │                   │                         │                      │
   │ 1. POST /upload   │                         │                      │
   │ ─────────────────>│                         │                      │
   │                   │ 2. 保存音频文件            │                      │
   │                   │    ${user.home}/uploads  │                      │
   │                   │                         │                      │
   │                   │ 3. 异步调用 Whisper       │                      │
   │                   │    ProcessBuilder       │                      │
   │                   │ ───────────────────────>│                      │
   │                   │                         │ 4. 加载 base 模型     │
   │                   │                         │ 5. FFmpeg 解码       │
   │                   │                         │ 6. Whisper 推理      │
   │                   │                         │                      │
   │                   │ 7. 返回转录 JSON         │                      │
   │                   │ <────────────────────────│                      │
   │                   │                         │                      │
   │                   │ 8. 调用 GLM-5           │                      │
   │                   │ ───────────────────────────────────────────────>│
   │                   │                         │                      │
   │                   │ 9. 返回会议纪要 JSON     │                      │
   │                   │ <───────────────────────────────────────────────│
   │                   │                         │                      │
   │                   │ 10. 保存到 MySQL        │                      │
   │                   │     UPDATE meeting      │                      │
   │                   │                         │                      │
   │ 11. GET /list     │                         │                      │
   │ <─────────────────│                         │                      │
   │                   │                         │                      │
```

### 4.2 关键数据结构

**转录结果 (Whisper 输出):**
```json
{
  "success": true,
  "text": "完整转录文本",
  "language": "zh",
  "segments": [
    {
      "start": "00:00:00",
      "end": "00:00:06",
      "text": "这段文字是示例"
    }
  ]
}
```

**会议纪要 (GLM-5 输出):**
```json
{
  "duration": "45分钟",
  "summary": "会议摘要...",
  "todos": [
    {"id": 1, "content": "任务描述", "done": false}
  ],
  "decisions": ["决策1", "决策2"],
  "speakers": [
    {
      "id": "speaker_1",
      "name": "说话人A",
      "color": "#409EFF",
      "segments": [
        {"time": "00:02:15", "text": "发言内容"}
      ]
    }
  ]
}
```

---

## 五、部署架构

### 5.1 本地开发环境

```
Windows 11
├── Node.js 18+ (前端)
├── Java 17 (后端)
├── Python 3.11 (Whisper)
│   ├── openai-whisper
│   └── ffmpeg 8.1
├── MySQL 8.0 (Docker)
└── Redis 7.x (可选)
```

### 5.2 端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 (Vite) | 5173 | 开发服务器 |
| 后端 (Spring Boot) | 8080 | REST API |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |

### 5.3 关键路径

| 路径 | 说明 |
|------|------|
| `meeting-record/frontend/` | Vue 前端代码 |
| `meeting-record/backend/` | Spring Boot 后端代码 |
| `meeting-record/backend/whisper/` | Python 转录脚本 |
| `meeting-record/docs/` | 项目文档 |
| `~/meeting-record/uploads/` | 音频文件存储 |

---

## 六、技术决策记录

### 6.1 已做决策

| 决策 | 选项 | 原因 |
|------|------|------|
| AI 语音识别 | Whisper (本地) | 免费、中文支持好、无网络依赖 |
| AI 纪要生成 | GLM-5 (在线) | 中文理解强、免费额度足够 |
| 前端框架 | Vue 3 | 响应式系统、Composition API |
| 后端框架 | Spring Boot 3.1.12 | 与 MyBatis Plus 3.5.6 兼容 |
| 数据库 | MySQL 8.0 | JSON 字段支持、成熟稳定 |
| 文件存储 | 本地文件系统 | 简单、无需额外服务 |

### 6.2 备选方案

| 场景 | 当前方案 | 备选方案 | 切换条件 |
|------|----------|----------|----------|
| 语音识别 | Whisper base | Whisper small/medium | 准确度不足时 |
| AI 生成 | GLM-5 | Claude/文心一言 | API 不稳定时 |
| 文件存储 | 本地 | 阿里云 OSS | 多机部署时 |
| 数据库 | MySQL | PostgreSQL | 需要高级 JSON 功能时 |

---

## 七、更新日志

| 日期 | 更新内容 | 更新人 |
|------|----------|--------|
| 2026-04-04 | 初始版本，记录 Whisper + GLM-5 架构 | Claude |
| | | |

---

## 八、参考文档

- [需求设计文档](./需求设计文档.md) - 功能需求和业务流程
- [UI设计规范](./UI设计规范.md) - 视觉设计和交互规范
- [CLAUDE.md](../CLAUDE.md) - 项目约定和开发规范
