# CLAUDE.md - 会议纪要应用

> 本文档记录用户的技术偏好和项目规范，用于指导AI助手后续开发工作。

---

## 用户偏好

### 技术栈偏好
| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 前端框架 | Vue 3 + Element Plus | 使用Vue3组合式API，Element UI组件库 |
| 图标方案 | SVG图标 | 不使用图标字体，统一使用SVG |
| 后端框架 | Spring Boot | Java生态，版本3.1.12（3.2.x与MyBatis Plus不兼容）|
| 数据库 | MySQL 8.0 | 关系型数据库 |
| 缓存 | Redis 7.x | 会话缓存、热点数据 |
| 部署方式 | Docker + Docker Compose | 容器化部署 |
| AI语音识别 | OpenAI Whisper (本地) | base 模型，CPU 推理，免费 |
| AI纪要生成 | GLM-5 API (在线) | https://apie.zhisuaninfo.com/v1 |

### 项目结构偏好
- 项目至少包含3个主文件夹：
  - `frontend/` - 前端代码 (Vue 3)
  - `backend/` - 后端代码 (Spring Boot)
  - `docs/` - 项目文档
- `CLAUDE.md` 必须放在项目根目录

### 代码规范
- 前端使用组合式API (Composition API)
- 图标使用独立SVG文件，存放于 `frontend/src/assets/icons/`
- 后端采用分层架构：Controller -> Service -> Mapper
- 数据库使用MyBatis或MyBatis-Plus

### 设计规范
- UI遵循Element Plus设计系统
- 主色调: #409EFF
- 侧边栏宽度: 260px
- 使用圆角卡片、阴影效果
- 响应式支持移动端

---

## 项目信息

### 会议纪要应用
**核心功能:**
1. 用户上传会议录音
2. AI音色分离识别说话人
3. 双Tab展示结果:
   - Tab 1: 会议整体纪要（持续时间、待办、敲定内容）
   - Tab 2: 按说话人分离的逐字稿（优化表达，去除啰嗦、空白、口吃）
4. 左侧边栏历史记录，支持搜索

**AI配置:**
- 密钥: sk-kimi-QIgK21NOEjZII14NJkBRd16cxCLbEqOzaGkxUzW32EHiv7cdyXU7jWLjzB3ZEyZ1
- 地址: https://api.kimi.com/coding/v1
- 模型: kimi-for-coding

---

## 开发流程约定

### 📋 开发前必读

**每次需求提出或准备开发时，必须先参考以下文档：**

| 文档 | 路径 | 用途 |
|------|------|------|
| **需求设计文档** | `docs/需求设计文档.md` | 功能需求、业务流程、接口定义 |
| **UI设计规范** | `docs/UI设计规范.md` | 视觉规范、组件样式、交互标准 |
| **技术架构文档** | `docs/architec.md` | 技术选型、模块关系、数据流转 |

> ⚠️ **重要**：不理解需求设计文档的业务流程，不得开始编码。  
> ⚠️ **重要**：不参考UI设计规范的样式标准，不得开始编码。  
> ⚠️ **重要**：不熟悉技术架构文档的模块关系，不得开始编码。

### 开发步骤

1. **需求分析** → 阅读 `docs/需求设计文档.md`
2. **UI设计确认** → 阅读 `docs/UI设计规范.md`
3. **技术方案** → 阅读 `docs/architec.md`
4. **编码实现** → 遵循本文档的代码规范
5. **更新文档** → 如有架构变更，同步更新 `docs/architec.md`

---

## 开发约定

### 第三方内容存储规范

**禁止在 C 盘下载/存储第三方内容。**

| 内容类型 | 存储位置 | 说明 |
|----------|----------|------|
| **项目缓存** | 项目路径下的 `cache/` 文件夹 | 如：`backend/cache/` 或项目根目录 `cache/` |
| **大模型/可复用资源** | 由用户指定磁盘 | 如：`E:\Models\` 或 `D:\Models\`，需询问用户 |
| **开发工具缓存** | 由用户指定磁盘 | 如：`E:\AboutDevelopment\MavenCache` |

**示例：**
- Whisper 模型 → `E:\Models\whisper\`
- pyannote 模型 → `E:\Models\huggingface\hub\`
- Maven 缓存 → `E:\AboutDevelopment\MavenCache\`
- 项目临时文件 → `D:\project\cache\temp\`

### 提交规范
- 功能开发: `feat: 描述`
- 修复bug: `fix: 描述`
- 文档更新: `docs: 描述`
- 样式调整: `style: 描述`

### 分支管理

**GitHub 仓库：** https://github.com/phuong0944298660-code/MeetingRecord.git

**分支策略：**
| 分支 | 用途 | 说明 |
|------|------|------|
| `main` | 生产分支 | 线上稳定版本，仅接受 UAT 合并 |
| `uat` | 预上线分支 | 预发布环境，仅接受 Develop 合并 |
| `develop` | 开发分支 | 日常开发，功能分支合并目标 |
| `feature/*` | 功能分支 | 新功能开发，从 develop 切出 |
| `bugfix/*` | 修复分支 | Bug 修复，从 develop 切出 |

**发布流程：**
```
feature/* 或 bugfix/* → develop → uat → main
```

**⚠️ 重要：** 所有云端提交（`git push`）必须由我本人发起。AI 助手仅协助本地代码修改和提交准备，不执行推送操作。

---

## 自动化脚本

### 已创建的脚本

| 脚本 | 路径 | 用途 |
|------|------|------|
| auto-fix.bat | `scripts/auto-fix.bat` | Windows 自动诊断修复 |
| auto-fix.ps1 | `scripts/auto-fix.ps1` | PowerShell 完整脚本 |
| auto-fix.sh | `scripts/auto-fix.sh` | Linux/macOS 脚本 |
| quick-start.bat | `scripts/quick-start.bat` | Windows 快速启动 |
| integration-test.ps1 | `scripts/integration-test.ps1` | API 集成测试 |

### 自动修复的问题

1. **Spring Boot 版本兼容性** - 自动降级 3.2.4 → 3.1.12
2. **端口占用** - 自动查找并释放占用端口的进程
3. **数据库连接** - 尝试启动 Docker 数据库
4. **Maven 路径** - 自动搜索常见安装路径

### 使用方法

```bash
# Windows - 自动诊断修复
cd scripts
auto-fix.bat

# Docker 一键部署
cd scripts
auto-fix.bat --docker

# 快速启动（仅启动服务，不检测）
cd scripts
quick-start.bat
```

---

## 云端推送备注模板

以后按以下模板编写提交内容，必须包含：**背景、目的、增删改内容、结果**

### 1. 功能开发提交
```
【类型】功能开发
【分支】feature/xxx → develop

【背景】
描述为什么要做这个功能，业务需求来源

【目的】
这个功能要解决什么问题，达到什么目标

【增删改内容】
- 新增：xxx 功能模块
- 修改：xxx 接口/页面
- 删除：无用代码 xxx
- 涉及文件：
  - frontend/src/views/xxx.vue
  - backend/src/xxx.java

【结果】
- 本地测试：通过/未通过
- 功能验证：已实现预期功能
- 后续计划：是否需要补充测试等
```

### 2. Bug 修复提交
```
【类型】Bug 修复
【分支】bugfix/xxx → develop

【背景】
描述 Bug 出现的场景、现象、影响范围

【目的】
修复该 Bug，恢复正常功能

【增删改内容】
- 修改：修复 xxx 逻辑
- 删除：错误代码 xxx
- 新增：异常处理 xxx
- 涉及文件：
  - backend/src/service/xxx.java

【结果】
- 修复验证：Bug 已修复，测试通过
- 回归测试：相关功能正常
- 影响评估：无其他副作用
```

### 3. 预上线合并
```
【类型】预上线发布
【分支】develop → uat
【版本号】v1.x.x

【背景】
开发阶段完成，进入 UAT 测试阶段

【目的】
将 develop 分支代码合并到 uat，供测试人员验证

【增删改内容】
- 新增功能：
  1. xxx 功能
  2. xxx 功能
- 修改优化：
  1. xxx 优化
- Bug 修复：
  1. 修复 xxx 问题
- 配置文件变更：无/有（如有需列出）

【结果】
- 代码审查：已通过
- 单元测试：全部通过
- 集成测试：待 UAT 验证
- 发布时间：计划 xxxx-xx-xx
```

### 4. 生产发布
```
【类型】生产发布
【分支】uat → main
【版本号】v1.x.x

【背景】
UAT 测试通过，准备发布到生产环境

【目的】
将验证通过的代码发布到线上，供用户使用

【增删改内容】
- 发布功能清单：
  1. xxx 功能上线
  2. xxx 优化生效
  3. xxx Bug 修复
- 数据库变更：无/有（SQL文件xxx）
- 配置变更：无/有（需更新xxx）

【结果】
- UAT 测试：全部通过
- 上线检查：各项检查完成
- 回滚方案：已准备，步骤xxx
- 发布后验证：待执行
```

### 5. 紧急修复
```
【类型】紧急修复
【分支】hotfix/xxx → main & develop

【背景】
生产环境出现紧急问题，影响用户正常使用
问题现象：xxx
影响范围：xxx
发现时间：xxx

【目的】
快速修复紧急问题，恢复生产环境正常运行

【增删改内容】
- 修改：紧急修复 xxx 问题
- 涉及文件：
  - backend/src/xxx.java
- 临时方案/永久方案说明

【结果】
- 修复验证：问题已修复
- 测试情况：紧急测试通过
- 生产验证：已验证正常
- 后续跟进：是否需要合并到 develop
```

---

*最后更新: 2026-04-09*  
*更新内容: 添加 GitHub 仓库链接、分支管理策略、云端推送备注模板*
