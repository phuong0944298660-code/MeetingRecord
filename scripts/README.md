# 自动化测试与修复脚本

这个目录包含自动化诊断和修复会议纪要应用的工具。

## 快速开始（推荐）

### 一键启动前后端（自动修复问题）

```bash
# 在项目根目录双击运行
start-all-simple.bat

# 或在 scripts 目录运行
scripts\start-all.bat
```

**脚本会自动：**
1. 检查环境（Java、Maven、Node.js）
2. 释放被占用的端口（8080、5173）
3. 检查并修复后端问题（缺少 JAR 则自动构建）
4. 检查并修复前端问题（缺少 node_modules 则自动安装）
5. 启动后端服务（http://localhost:8080）
6. 启动前端服务（http://localhost:5173）

---

## 文件说明

| 文件 | 用途 | 位置 |
|------|------|------|
| `start-all.bat` | 一键启动前后端（推荐） | scripts/ |
| `auto-fix.bat` | PowerShell 自动修复入口 | scripts/ |
| `auto-fix.ps1` | PowerShell 完整修复脚本 | scripts/ |
| `auto-fix.sh` | Linux/macOS 修复脚本 | scripts/ |
| `integration-test.ps1` | API 集成测试 | scripts/ |

---

## 使用方法

### 方式一：一键启动所有服务（推荐）

```bash
cd meeting-record/scripts
start-all.bat
```

此脚本会自动检查并修复：Java、Maven、Node.js 环境，端口占用，缺少的 JAR 和前端依赖。

### 方式三：Docker 部署

```bash
docker-compose up --build -d
```

---

## 自动修复的问题

### 1. 端口占用
- **症状**: `Address already in use: bind`
- **修复**: 自动查找并终止占用 8080/5173 的进程

### 2. 后端 JAR 不存在
- **症状**: 找不到 `meeting-record-backend-1.0.0.jar`
- **修复**: 自动运行 `mvn clean package -DskipTests`

### 3. 前端依赖缺失
- **症状**: 找不到 `node_modules`
- **修复**: 自动运行 `npm install`

### 4. Spring Boot 版本兼容性
- **症状**: `Invalid value type for attribute 'factoryBeanObjectType'`
- **修复**: 自动降级 Spring Boot 3.2.4 → 3.1.12

### 5. Maven 未找到
- **症状**: `mvn 不是内部或外部命令`
- **修复**: 自动搜索 Chocolatey 安装路径

---

## 访问地址

启动成功后：

- **前端界面**: http://localhost:5173
- **后端 API**: http://localhost:8080
- **API 文档**: http://localhost:8080/swagger-ui.html

---

## 手动操作

如果自动脚本失败，可以手动执行：

### 构建后端
```bash
cd backend
mvn clean package -DskipTests
```

### 安装前端依赖
```bash
cd frontend
npm install
```

### 启动后端
```bash
cd backend
java -jar target/meeting-record-backend-1.0.0.jar
```

### 启动前端
```bash
cd frontend
npm run dev
```

### 启动数据库
```bash
docker-compose up -d mysql redis
```

---

## 常见问题

### Q: 脚本一闪而过
**A**: 从 CMD 运行脚本查看错误信息：
```bash
cd meeting-record/scripts
start-all.bat
```

### Q: 端口被占用无法释放
**A**: 以管理员身份运行脚本，或手动关闭占用端口的程序

### Q: 构建失败
**A**: 检查 `backend/pom.xml` 中的 Spring Boot 版本是否为 3.1.12

### Q: 前端启动后空白
**A**: 检查后端是否正常运行，打开浏览器控制台查看错误

### Q: 数据库连接错误
**A**: 修改 `backend/src/main/resources/application.yml` 中的数据库配置
