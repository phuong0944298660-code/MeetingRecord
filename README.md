# 会议纪要应用

智能语音会议纪要系统，支持录音上传、语音识别、说话人分离、自动生成会议纪要。

## 技术栈

- **前端**: Vue 3 + Element Plus + Pinia
- **后端**: Spring Boot + MyBatis Plus + MySQL + Redis
- **AI**: Kimi API (语音识别 + 自然语言处理)
- **部署**: Docker + Docker Compose

## 项目结构

```
meeting-record/
├── CLAUDE.md              # 项目偏好记录
├── docker-compose.yml     # Docker编排
├── frontend/              # Vue前端
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/           # API接口
│       ├── assets/        # 静态资源
│       ├── components/    # 组件
│       ├── stores/        # Pinia状态管理
│       └── main.js
├── backend/               # Spring Boot后端
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── main/java/com/meeting/
│           ├── config/    # 配置类
│           ├── controller/# 控制器
│           ├── dto/       # 数据传输
│           ├── entity/    # 实体类
│           ├── mapper/    # 数据访问
│           ├── service/   # 服务层
│           └── util/      # 工具类
├── docs/                  # 项目文档
│   ├── 需求设计文档.md
│   ├── UI设计规范.md
│   └── 组件展示.html
└── mysql/
    └── init.sql           # 数据库初始化
```

## 快速开始

### 方式一：Docker部署（推荐）

```bash
# 启动所有服务
docker-compose up -d

# 访问应用
# 前端: http://localhost
# 后端API: http://localhost:8080
```

### 方式二：本地开发

#### 1. 启动数据库
```bash
# 启动MySQL和Redis
docker-compose up -d mysql redis
```

#### 2. 启动后端
```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

#### 3. 启动前端
```bash
cd frontend
npm install
npm run dev
```

## 功能特性

### 核心功能
- [x] 录音文件上传（支持 MP3, WAV, M4A, AAC, OGG）
- [x] 语音识别与说话人分离
- [x] 自动生成会议纪要（摘要、待办、决策）
- [x] 逐字稿按说话人展示
- [x] 历史会议管理与搜索

### 用户功能
- [x] 用户注册/登录（JWT认证）
- [x] 待办事项勾选完成
- [x] 会议删除

## API接口

### 认证接口
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

### 会议接口
- `GET /api/meeting/list` - 获取会议列表
- `GET /api/meeting/{id}` - 获取会议详情
- `POST /api/meeting/upload` - 上传会议录音
- `POST /api/meeting/{id}/todo/{todoId}/toggle` - 切换待办状态
- `DELETE /api/meeting/{id}` - 删除会议

## 配置说明

### 后端配置 (application.yml)
```yaml
# 数据库
spring.datasource.url: jdbc:mysql://localhost:3306/meeting_record
spring.datasource.username: root
spring.datasource.password: 123456

# Redis
spring.data.redis.host: localhost
spring.data.redis.port: 6379

# Kimi API
meeting.kimi.api-key: your-api-key
meeting.kimi.base-url: https://api.kimi.com/coding/v1
meeting.kimi.model: kimi-for-coding

# 文件上传
meeting.upload.path: ./uploads
```

### 前端配置 (vite.config.js)
```javascript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 开发指南

### 前端开发
```bash
cd frontend
npm install
npm run dev
```

### 后端开发
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 数据库变更
修改 `mysql/init.sql` 后重启 MySQL 容器：
```bash
docker-compose restart mysql
```

## 环境要求

- Java 17+
- Node.js 18+
- Maven 3.9+
- MySQL 8.0
- Redis 7.x

## 部署说明

生产环境部署请修改以下配置：
1. 修改 `application.yml` 中的数据库密码和JWT密钥
2. 修改 `docker-compose.yml` 中的环境变量
3. 配置HTTPS证书
4. 配置防火墙规则

## 许可证

MIT
