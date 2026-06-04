# TiHu 鹈鹕图书分享交流平台

TiHu 是一个基于 JavaFX + Spring Boot 的桌面端图书分享交流平台，覆盖图书浏览、评分评论、收藏书单、用户关注、私信交流和管理员后台管理等场景。项目采用前后端分离结构：前端提供桌面交互界面，后端提供 REST API、登录鉴权、数据持久化和业务规则校验。

本项目为 2025-2026 第二学期 Java 课程设计。

## 项目成员

| 成员 | 职责 |
| --- | --- |
| 杨佳明 | 项目总负责人，确定项目功能边界与开发节奏，完成整体测试与 Bug 修复；引入 Sa-Token 简化登录、会话和角色鉴权，引入 MyBatis-Plus 简化数据库访问与通用 CRUD。 |
| 杨盛超 | 后端分负责人，统一前后端 API 接口规范，完善全局异常捕获和统一响应格式；配合前端 Mock 测试、远程接口联调和项目漏洞排查。 |
| 薛心昊 | 数据库分负责人，设计 MySQL 数据库表结构，承载用户、图书、评论、收藏、书单、关注和私信等核心数据；结合 Redis 提升非关系型和高频数据读取效率，编写项目报告。 |
| 徐浩腾 | 前端分负责人，使用 Scene Builder 设计 JavaFX 页面，完成主要界面布局、UI 美化和交互优化；调整日间模式与夜间模式配色，提升整体视觉一致性。 |

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Java 21, JavaFX 21.0.6, FXML, Scene Builder, Maven |
| 后端 | Java 21, Spring Boot 4, MyBatis-Plus, Sa-Token, Maven |
| 数据库 | MySQL, Redis |
| 通信 | REST API, JSON, Authorization Token |
| 测试 | JUnit 5, 前端 Mock 服务, FXML 加载测试, API 合同测试 |

## 项目结构

```text
TiHu/
├── README.md
├── backend/
│   ├── mvnw / mvnw.cmd
│   ├── pom.xml
│   ├── schema.sql                    # 数据库初始化脚本，便于手动导入
│   ├── src/main/resources/
│   │   ├── application-dev.yaml       # 开发环境数据库、Redis、端口配置
│   │   └── schema.sql                 # Spring Boot 启动时使用的初始化脚本
│   └── README.md
├── frontend/
│   ├── mvnw / mvnw.cmd
│   ├── pom.xml
│   ├── docs/frontend-api.md
│   ├── src/main/java/com/tihu/frontend/
│   └── README.md
└── dist/
    └── README.md
```

## 环境准备

请先在目标电脑安装并确认可用：

- JDK 21 或以上：`java -version`
- MySQL 8 或兼容版本：`mysql --version`
- Redis：`redis-server --version`
- Maven 可选；项目已包含 Maven Wrapper，Windows 使用 `mvnw.cmd`，macOS/Linux 使用 `./mvnw`

后端默认配置：

```text
MySQL: localhost:3306/tihu
Redis: 127.0.0.1:6379
API:   http://localhost:9090/api
```

如果本机 MySQL 用户名、密码、端口或 Redis 配置不同，请修改：

```text
backend/src/main/resources/application-dev.yaml
```

默认 MySQL 用户名和密码为 `root` / `root`。

## 快速启动

以下命令都假设你已经进入项目根目录，也就是包含 `backend`、`frontend`、`README.md` 的目录。项目可以放在任意路径，不需要放在某个固定电脑目录。

### 1. 初始化数据库

Windows PowerShell：

```powershell
cd backend
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS tihu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
cmd /c "mysql -u root -p tihu < schema.sql"
```

macOS/Linux：

```bash
cd backend
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS tihu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p tihu < schema.sql
```

说明：开发配置中 `spring.sql.init.mode=always` 且 `schema-locations=classpath:schema.sql`，后端启动时也会尝试初始化表结构；手动导入用于提前确认数据库连接和表结构。

### 2. 启动后端

Windows PowerShell：

```powershell
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

macOS/Linux：

```bash
cd backend
chmod +x mvnw
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

启动成功后，后端接口基础地址为：

```text
http://localhost:9090/api
```

### 3. 启动前端

另开一个终端，回到项目根目录后执行。

Windows PowerShell：

```powershell
cd frontend
.\mvnw.cmd javafx:run
```

macOS/Linux：

```bash
cd frontend
chmod +x mvnw
./mvnw javafx:run
```

也可以在 IntelliJ IDEA 中打开 `frontend` 模块并运行：

```text
com.tihu.frontend.MainApplication
```

## 前端远程配置

前端默认请求 `http://localhost:9090/api`。如果后端不在本机或端口不同，可以覆盖后端地址。

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

默认开发模式下，部分远程接口失败会回退到本地 Mock，便于离线演示。联调和验收时建议开启严格远程模式。

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

Mock 模式下可使用以下账号：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Admin123` | 管理员 |
| `alice` | `Alice123` | 普通用户 |
| `bob` | `Bob12345` | 普通用户 |

管理员注册邀请码：

```text
123456
```

远程后端模式下，请以实际数据库中的用户为准，也可以通过注册页面新建用户。

## 核心功能

| 模块 | 功能 |
| --- | --- |
| 用户认证 | 普通注册、管理员邀请码注册、登录、登出、Token 鉴权、封禁登录提示 |
| 首页 | 项目简介、团队贡献展示、日间/夜间主题适配 |
| 图书浏览 | 图书列表、分页、按书名搜索、按标签筛选、按评分排序、封面展示 |
| 图书详情 | 图书简介、封面、标签、评分统计、我的评分、收藏数、评论区 |
| 图书管理 | 管理员新增、修改、删除图书，支持上传、修改、清空图书封面 |
| 评分评论 | 1 到 10 分评分，发表评论、回复、删除、点赞、点踩、取消投票 |
| 收藏 | 收藏图书、取消收藏、收藏列表、收藏状态检查、收藏时间展示 |
| 书单 | 创建书单、公开/私密切换、按 ID 或书名添加图书、移除图书、删除书单 |
| 用户主页 | 用户头像、关注数、粉丝数、用户评论、公开书单、关注/取关 |
| 关注粉丝 | 查询关注列表、粉丝列表，支持查看指定用户关系 |
| 私信 | 会话列表、一对一聊天、按用户 ID 或用户名发起私信 |
| 管理后台 | 用户列表、封禁/解封、授予管理员权限、评论管理 |
| 前端 Mock | 后端不可用时可用本地 Mock 数据演示，支持持久化测试状态 |

## API 约定

前后端接口以以下文档为准：

```text
frontend/docs/frontend-api.md
```

统一响应格式：

```json
{
  "code": 200,
  "message": "OK",
  "data": {}
}
```

主要约定：

- 成功时 `code=200`。
- 业务数据放在 `data` 中。
- 错误信息放在 `message` 中。
- 登录成功后后端返回 `Authorization`，前端后续请求自动携带。
- 时间字段使用 ISO-8601 本地时间，例如 `2026-06-04T15:00:00`。
- 图书封面和用户头像支持 URL 或 `data:image/...;base64,...`。

## 测试与验证

后端测试：

```powershell
cd backend
.\mvnw.cmd test
```

前端测试：

```powershell
cd frontend
.\mvnw.cmd test
```

macOS/Linux 将 `.\mvnw.cmd` 替换为 `./mvnw`。

## 常见问题

### 后端启动失败

检查 MySQL 和 Redis 是否已启动，并确认 `backend/src/main/resources/application-dev.yaml` 中数据库账号、密码、端口正确。

### `mysql` 命令不可用

说明 MySQL 客户端没有加入系统 PATH。可以把 MySQL 的 `bin` 目录加入 PATH，或在图形化数据库工具中执行 `backend/schema.sql`。

### 前端登录后没有远程数据

确认后端是否监听在 `9090` 端口，接口是否带 `/api` 前缀。联调时建议开启 `TIHU_REMOTE_STRICT=true`，避免 Mock 回退掩盖后端问题。

### Redis 暂时不用怎么办

当前后端依赖 Redis。请启动 Redis，或按实际开发需要调整后端配置和相关依赖后再运行。

### 图片或头像不显示

后端应返回可直接展示的图片 URL 或 `data:image/...;base64,...`。清空封面或头像时返回空字符串或 `null`。

### 管理员功能不可用

确认当前登录用户角色为 `ADMIN`。可使用管理员邀请码注册，或由已有管理员在用户管理页授予管理员权限。

## 文档维护

功能或接口变更后，建议同步：

1. `frontend/docs/frontend-api.md`
2. `README.md`
3. `frontend/README.md`
4. `backend/README.md`
5. `dist/README.md`
6. 数据库脚本 `backend/schema.sql` 和 `backend/src/main/resources/schema.sql`
