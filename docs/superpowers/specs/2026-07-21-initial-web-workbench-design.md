# Java 面试知识工坊首个 Web 闭环设计

## 目标

建立第一个前后端真实贯通的可用功能：用户上传单个 UTF-8 Markdown 文件，后端完成来源登记、版本识别、标题解析和 chunk 切分，前端展示导入状态、内容版本与可追踪 chunk。

当前闭环用于验证知识文档处理，不伪造尚未实现的向量检索、RAG 问答、评估或持久化能力。

## 架构选择

### 后端

- 增加 Spring MVC 与 multipart 支持，保持单体 Spring Boot。
- 独立项目开发端口使用 `18080`，避开本机已有 Tomcat 占用的 `8080`。
- 新增字符串内容导入器，作为文件导入与 HTTP 上传的共同核心，避免 Web 层创建临时文件或复制解析逻辑。
- HTTP 接口接收 `file` 与显式逻辑来源 `source`，返回结构化文档版本和 chunk 元数据。
- 登记仍使用现有进程内 `MarkdownImportRegistry`；接口和页面必须明确该限制。

### 前端

在仓库内新增独立 `frontend/`：

```text
frontend/
├─ src/api/          HTTP 客户端、接口和类型
├─ src/components/   导入面板、处理链与 chunk 账页
├─ src/router/       页面路由
├─ src/styles/       设计变量和全局样式
├─ src/views/        知识工坊页面
├─ App.vue
└─ main.ts
```

采用 Vue 3、Vite、TypeScript、Vue Router、Axios 和 Sass。参考 `shoka-blog` 的 Vite 别名、API 分层、视图与组件分离、Sass 主题变量和路由懒加载；不复制其 Pinia、Element Plus、Naive UI、编辑器、聊天、音乐或博客组件。当前只有一个页面，没有真实共享状态，因此不引入 Pinia。

## HTTP 契约

### 请求

```text
POST /api/knowledge/documents/import
Content-Type: multipart/form-data

file: Markdown 文件
source: java/basic.md
```

### 成功响应

```json
{
  "source": "java/basic.md",
  "version": "sha256",
  "imported": true,
  "chunkCount": 2,
  "chunks": [
    {
      "order": 0,
      "sectionOrder": 0,
      "headingLevel": 1,
      "headingPath": ["Java"],
      "content": "正文"
    }
  ]
}
```

相同来源与版本重复上传时返回 `imported=false`、相同版本和空 `chunks`。调用方输入错误返回 HTTP 400 与简体中文 `message`；文件读取或服务器异常不伪装成成功结果。

## 页面任务

页面的唯一任务是导入一份 Markdown 资料并检查它如何被系统理解。首屏包含：

- 项目标识与当前能力边界。
- 文件选择与拖放区域。
- 可编辑的逻辑来源；选择文件后默认使用文件名，但用户可以改为稳定业务路径。
- “解析并登记”主操作。
- `SOURCE → HASH → SECTION → CHUNK` 处理链脊柱。
- 导入结果摘要、版本指纹和 chunk 账页。
- 空状态、处理中、重复版本、成功和失败提示。

## 视觉方向

主题是“Java 面试知识工坊”，不是通用后台。

### 色彩

- 纸白 `#F6F4EE`：页面基底。
- 墨绿 `#173C3A`：主要文字与顶栏。
- 青玉 `#4F8F89`：主操作、流程完成态。
- 朱砂 `#C75A45`：版本、重复和重点提示。
- 书页 `#FFFCF6`：卡片表面。
- 铅灰 `#D8D4C9`：边界与次要信息。

暗色主题使用系统偏好自动切换，不在第一版增加主题开关。

### 字体

- 标题：`Noto Serif SC`、`Songti SC`、系统中文衬线回退。
- 正文：`Inter`、`PingFang SC`、`Microsoft YaHei`、系统无衬线回退。
- 数据：`JetBrains Mono`、`Cascadia Code`、系统等宽回退。

不依赖远程字体，避免本地开发与离线环境出现阻塞。

### 布局

```text
┌──────────────────────────────────────────────────────────────┐
│ Java Interview RAG                         session / memory  │
├──────────────────────────────────────────────────────────────┤
│ 把 Markdown 变成可追踪的知识切片           SOURCE → ...      │
├───────────────────────┬──────────────────────────────────────┤
│ 文件与逻辑来源        │ 版本摘要 / chunk 账页                │
│ 选择、拖放、提交      │ 路径、顺序、正文                     │
└───────────────────────┴──────────────────────────────────────┘
```

桌面为左右工作台，移动端按标题、处理链、导入、结果顺序堆叠。圆角、阴影和动效保持克制；唯一强记忆点是处理链脊柱。支持可见键盘焦点，并尊重 `prefers-reduced-motion`。

## 错误与状态

- 未选择文件：前端阻止提交并指出下一步。
- 来源为空：前端提示，后端仍执行校验并返回 400。
- 后端不可达：明确显示“无法连接后端服务”，保留已选文件与来源。
- 重复版本：显示已识别的版本指纹，不渲染空白 chunk 卡片。
- 页面刷新：结果丢失，符合当前无持久化事实；页面明确标注“当前会话内登记”。

## 验证

- 后端：MockMvc 覆盖首次 multipart 导入、重复导入与非法来源；现有 Markdown 链路测试继续通过。
- 前端：TypeScript 类型检查与 Vite 生产构建通过。
- 集成：启动 Spring Boot 与 Vite，通过 Playwright 验证文件选择、来源填写、提交、成功结果与响应式布局，并保存截图。

## 明确不做

- 用户登录、权限、数据库持久化、目录批量导入。
- 向量库、Embedding、RAG 问答和评估页面。
- 复制 `shoka-blog` 的博客导航、Live2D、音乐、聊天或多 UI 框架。
- 为一个页面提前建立通用组件库或大型状态管理层。
