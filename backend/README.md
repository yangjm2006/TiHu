# TiHu 后端服务

TiHu 后端是图书分享交流平台的 REST API 服务，负责用户认证、图书管理、评分评论、收藏书单、关注粉丝、私信会话和管理员后台能力。前端 JavaFX 客户端默认通过 `http://localhost:9090/api` 调用本服务。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 运行环境 | Java 21 |
| Web 框架 | Spring Boot 4 |
| ORM | MyBatis-Plus |
| 权限认证 | Sa-Token |
| 数据库 | MySQL |
| 缓存 | Redis |
| 构建工具 | Maven Wrapper |

## 目录说明

```text
backend/
├── mvnw / mvnw.cmd
├── pom.xml
├── schema.sql
├── src/main/java/com/tihu/backend/
├── src/main/resources/application-dev.yaml
├── src/main/resources/schema.sql
└── README.md
```

项目可以放在任意目录。以下命令都在 `backend` 目录中执行，不需要使用固定的本机绝对路径。

## 环境准备

请确认目标电脑已安装：

- JDK 21 或以上
- MySQL 8 或兼容版本
- Redis

开发配置文件：

```text
src/main/resources/application-dev.yaml
```

默认配置：

```text
MySQL: localhost:3306/tihu
MySQL 用户名/密码: root/root
Redis: 127.0.0.1:6379
后端端口: 9090
```

如果本机配置不同，请先修改 `application-dev.yaml`。

## 快速启动

### 1. 初始化数据库

在 `backend` 目录中创建数据库并导入脚本。

Windows PowerShell：

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS tihu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
cmd /c "mysql -u root -p tihu < schema.sql"
```

macOS/Linux：

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS tihu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p tihu < schema.sql
```

说明：`src/main/resources/schema.sql` 也会随应用启动被 Spring Boot 读取；手动导入主要用于提前初始化和排查数据库问题。

### 2. 启动服务

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

macOS/Linux：

```bash
chmod +x mvnw
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

启动后 API 基础地址：

```text
http://localhost:9090/api
```

## 功能模块

| 模块 | 接口能力 |
| --- | --- |
| 用户认证 | 注册、管理员邀请码注册、登录、登出、当前用户信息 |
| 用户资料 | 修改用户名、密码、头像，按用户名查询主页 |
| 图书 | 图书列表、搜索、详情、新增、编辑、删除、标签 |
| 评分 | 1 到 10 分评分、我的评分、评分统计 |
| 评论 | 发表评论、回复、删除、点赞、点踩、管理员评论管理 |
| 收藏 | 收藏图书、取消收藏、收藏列表、收藏状态检查 |
| 书单 | 创建书单、公开/私密、添加/移除图书、删除书单 |
| 关注 | 关注、取消关注、关注列表、粉丝列表 |
| 私信 | 发送私信、会话列表、会话消息 |
| 管理员 | 全部用户管理、封禁/解封用户、授予管理员权限 |

## API 文档

前后端联调以客户端文档为准：

```text
../frontend/docs/frontend-api.md
```

该文档按前端 `RemoteBackendService` 的实际调用路径整理，包含请求路径、参数、响应字段和兼容字段。

## 关键接口约定

- 统一响应建议使用 `code`、`message`、`data`。
- 成功响应使用 `code=200`。
- 登录成功后如返回 `Authorization` 响应头，前端会在后续请求中携带。
- 时间字段接口传输推荐 ISO-8601 本地时间，例如 `2026-06-04T15:00:00`。
- 前端页面显示日期统一为 `yyyy-MM-dd HH:mm:ss`，后端不要返回小数秒。
- 书单按书名添加图书时必须完整书名精确匹配，找不到返回“书名不存在”。
- 用户解封必须持久化清空封禁截止时间字段，后续用户管理列表应返回 `null`。
- 管理员用户列表按 `created_at_asc` 排序时，最早注册用户排在最上面。

## 测试

Windows PowerShell：

```powershell
.\mvnw.cmd test
```

macOS/Linux：

```bash
./mvnw test
```

## 常见问题

### 数据库连接失败

检查 MySQL 是否启动、数据库是否已按 `schema.sql` 初始化，并确认 `application-dev.yaml` 中账号密码正确。

### Redis 连接失败

检查 Redis 是否启动，或按当前开发环境调整 Redis 主机、端口和密码。

### 前端提示接口失败

检查后端是否监听在 `9090` 端口，接口是否带 `/api` 前缀，响应是否符合 `code=200 + data/result` 的结构。

### `mysql` 命令不可用

说明 MySQL 客户端没有加入系统 PATH。可以把 MySQL 的 `bin` 目录加入 PATH，或在图形化数据库工具中执行 `schema.sql`。

### 解封后前端仍显示封禁时间

解封接口不能只改用户状态，还必须把数据库中的封禁截止时间写为 `NULL`。否则前端重新查询用户管理列表时仍会显示旧封禁时间。
