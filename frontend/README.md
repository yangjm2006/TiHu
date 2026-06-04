# TiHu 前端客户端

TiHu 前端是 JavaFX 桌面客户端，配套 Spring Boot 后端使用。系统覆盖图书浏览、评分评论、收藏书单、关注粉丝、私信会话和管理员后台管理；前端优先调用远程 API，也保留本地 Mock 数据用于离线演示和测试。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 运行环境 | Java 21 |
| UI | JavaFX 21.0.6, FXML, Scene Builder |
| 构建工具 | Maven Wrapper |
| 接口 | HTTP REST, JSON |
| 测试 | JUnit 5, FXML 加载测试, API 合同测试 |

## 项目结构

```text
frontend/
├── mvnw / mvnw.cmd
├── pom.xml
├── docs/frontend-api.md
├── src/main/java/com/tihu/frontend/
│   ├── MainApplication.java
│   ├── controller/
│   ├── request/ApiClient.java
│   ├── service/
│   └── utils/
├── src/main/resources/com/tihu/frontend/
│   ├── *-view.fxml
│   ├── app-theme.css
│   └── app-icon.png
└── README.md
```

项目可以放在任意目录。以下命令都在 `frontend` 目录中执行，不需要使用固定的本机绝对路径。

## 环境准备

请确认目标电脑已安装：

- JDK 21 或以上
- 后端服务已启动，或允许前端使用本地 Mock 数据

后端默认地址：

```text
http://localhost:9090/api
```

## 快速启动

如果需要连接真实后端，请先启动 `../backend` 服务。

Windows PowerShell：

```powershell
.\mvnw.cmd javafx:run
```

macOS/Linux：

```bash
chmod +x mvnw
./mvnw javafx:run
```

也可以在 IntelliJ IDEA 中打开 `frontend` 模块并运行：

```text
com.tihu.frontend.MainApplication
```

## 远程 API 配置

前端默认请求：

```text
http://localhost:9090/api
```

如果后端不在本机或端口不同，可以覆盖后端地址。

Windows PowerShell：

```powershell
$env:TIHU_BACKEND_BASE_URL="http://服务器地址:9090/api"
.\mvnw.cmd javafx:run
```

macOS/Linux：

```bash
TIHU_BACKEND_BASE_URL="http://服务器地址:9090/api" ./mvnw javafx:run
```

也可以使用 JVM 参数：

```powershell
.\mvnw.cmd javafx:run -Dtihu.backend.base-url=http://服务器地址:9090/api
```

默认模式下，远程接口不可用时部分功能会回退到本地 Mock，便于前端离线演示。联调后端时建议开启严格远程模式。

Windows PowerShell：

```powershell
$env:TIHU_REMOTE_STRICT="true"
.\mvnw.cmd javafx:run
```

macOS/Linux：

```bash
TIHU_REMOTE_STRICT="true" ./mvnw javafx:run
```

或使用 JVM 参数：

```powershell
.\mvnw.cmd javafx:run -Dtihu.remote.strict=true
```

## 测试账号

本地 Mock 模式可使用：

| 用户名 | 密码 | 角色 | 用途 |
| --- | --- | --- | --- |
| `admin` | `Admin123` | 管理员 | 图书、用户、评论管理 |
| `alice` | `Alice123` | 普通用户 | 图书浏览、收藏、评论 |
| `bob` | `Bob12345` | 普通用户 | 关注、主页、私信演示 |

管理员注册邀请码：

```text
123456
```

连接真实后端时，请以数据库中的账号为准，也可以通过注册页面新建普通用户。

## 功能清单

| 模块 | 功能 |
| --- | --- |
| 登录注册 | 普通用户注册、管理员邀请码注册、登录鉴权、封禁登录提示 |
| 首页 | 品牌首页、日间/夜间主题、窗口图标 |
| 图书 | 图书列表、书名搜索、标签筛选、评分排序、分页展示 |
| 图书详情 | 图书信息、封面、标签、评分统计、我的评分、收藏状态 |
| 评论 | 发表评论、回复评论、删除自己的评论、点赞/点踩/取消投票 |
| 收藏 | 查看收藏、搜索收藏、分页、取消收藏 |
| 书单 | 创建书单、公开/私密、添加/移除图书、删除书单 |
| 用户主页 | 查看用户信息、评论、公开书单，双击书单进入详情 |
| 关注粉丝 | 关注、取消关注、查看关注列表和粉丝列表 |
| 私信 | 会话列表、一对一聊天、按用户名发起会话 |
| 管理后台 | 图书新增/编辑/删除，用户管理、封禁/解封、授予管理员，评论管理 |

## 关键交互规则

- 所有页面展示日期统一为 `yyyy-MM-dd HH:mm:ss`。
- 图书列表每页展示 9 本书。
- 收藏页面每页展示 9 本书。
- 我的书单按书名添加图书时必须完整书名精确匹配；找不到时显示“书名不存在”。
- 管理员用户管理页按注册时间从旧到新排序，最早注册的用户在最上面。
- 用户解封后，后端需要清空封禁截止时间；前端再次查询到空值时显示“正常”。
- 管理用户页面双击用户可打开用户主页，用户主页提供返回用户管理按钮。

## API 文档

前端实际调用接口以 [docs/frontend-api.md](docs/frontend-api.md) 为准。

文档包含：

- 通用请求、响应、分页和时间字段约定
- 用户、图书、评分、评论、收藏、书单、用户主页、关注、私信、管理员接口
- 前端兼容字段说明
- 前端实际调用接口清单

## 开发与验证

前端测试：

```powershell
.\mvnw.cmd test
```

macOS/Linux：

```bash
./mvnw test
```

清理重编译：

```powershell
.\mvnw.cmd clean compile
```

macOS/Linux：

```bash
./mvnw clean compile
```

后端测试请进入 `../backend` 后执行对应模块的 Maven Wrapper。

## 常见问题

### 前端登录失败

- 确认后端是否在 `http://localhost:9090/api` 运行。
- 确认账号密码正确。
- 如果正在联调后端，建议开启 `TIHU_REMOTE_STRICT=true`，避免 Mock 回退掩盖接口问题。

### 页面数据和数据库不一致

- 检查前端是否实际连上远程后端。
- 检查后端接口是否返回统一响应格式：`code=200` 且数据在 `data` 或 `result` 中。
- 管理员解封用户时，后端必须把封禁时间字段写为 `NULL`。

### 图片或头像不显示

- 前端支持 `data:image/...;base64,...` 或可访问的图片 URL。
- 清空封面/头像时前端会发送空字符串，后端应保存为空并返回空值。

## 文档维护

功能或接口变化后，优先同步：

1. [docs/frontend-api.md](docs/frontend-api.md)
2. 本 README
3. `../README.md`
4. `../backend/README.md`
