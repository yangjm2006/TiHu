# TiHu 鹈鹕图书分享交流平台

TiHu 是一个基于 JavaFX 和 Spring Boot 的桌面端图书分享交流平台。项目采用前后端分离架构：前端提供 JavaFX 桌面界面，后端提供 REST API、登录鉴权、业务规则校验和数据持久化。

本项目用于 2025-2026 第二学期 Java 课程设计。

## 项目成员

| 成员 | 分工 |
| --- | --- |
| 杨佳明 | 项目总负责人，负责功能边界、开发节奏、整体测试和 Bug 修复；引入 Sa-Token 与 MyBatis-Plus。 |
| 杨盛超 | 后端分负责人，负责前后端 API 规范、统一响应、异常处理、远程接口联调和后端问题排查。 |
| 薛心昊 | 数据库分负责人，负责 MySQL 表结构设计、核心数据建模、Redis 辅助能力和项目报告。 |
| 徐浩腾 | 前端分负责人，负责 JavaFX 页面、FXML 布局、Scene Builder 设计、主题配色和交互优化。 |

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Java 21, JavaFX 21.0.6, FXML, Scene Builder, Maven Wrapper |
| 后端 | Java 21, Spring Boot 4, MyBatis-Plus, Sa-Token, Maven Wrapper |
| 数据 | 远程 MySQL, 本地 Redis |
| 通信 | HTTP REST, JSON, Authorization Token |
| 测试 | JUnit 5, 前端 Mock 服务, FXML 加载测试, API 合同测试 |

## 当前运行配置

| 配置项 | 当前值 | 说明 |
| --- | --- | --- |
| 后端端口 | `22224` | 前端访问本机后端的端口 |
| API 基础地址 | `http://localhost:22224/api` | 前端默认请求地址 |
| MySQL 主机 | `202.194.14.120:3306` | 老师提供的远程 MySQL Server |
| 数据库 | `java_3_14` | 9-12 班、14 组团队库 |
| 数据库账号 | `java_3_14` | 远程 MySQL 用户名 |
| 数据库密码 | `JavaP314@` | 远程 MySQL 密码 |
| Redis | `127.0.0.1:6379` | 本机 Redis，后端启动需要 |

后端访问流程：

```text
JavaFX 前端 -> http://localhost:22224/api -> Spring Boot 后端 -> 202.194.14.120:3306/java_3_14
```

## 项目结构

```text
TiHu/
├── README.md
├── backend/
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   ├── schema.sql
│   ├── src/main/java/com/tihu/backend/
│   ├── src/main/resources/application-dev.yaml
│   ├── src/main/resources/application-prod.yaml
│   └── README.md
├── frontend/
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   ├── docs/frontend-api.md
│   ├── src/main/java/com/tihu/frontend/
│   ├── src/main/resources/com/tihu/frontend/
│   └── README.md
└── dist/
    ├── backend-0.0.1-SNAPSHOT.jar
    ├── frontend-1.0-SNAPSHOT-all.jar
    ├── tihu-database.sql
    ├── TiHu-submit.zip
    └── README.md
```

## 环境准备

请先确认目标电脑具备：

- JDK 21 或以上：`java -version`
- Redis：`redis-server --version`
- MySQL 客户端或 MySQL Workbench：用于检查远程数据库，不要求本机安装 MySQL Server
- Maven 可选：项目已包含 Maven Wrapper

如果无法访问 `202.194.14.120`，请先确认网络环境是否允许访问学校数据库服务器。

## 快速启动

以下命令都从项目根目录 `D:\Desktop\TiHu-master` 或对应模块目录执行。

### 1. 检查远程数据库

```powershell
mysql -h 202.194.14.120 -P 3306 -u java_3_14 -pJavaP314@ java_3_14 -e "SHOW TABLES;"
```

如果命令不可用，请在 MySQL Workbench 中使用同样的主机、用户名、密码和默认 schema 连接。

### 2. 启动 Redis

后端依赖 Redis。请确认本机 Redis 正在监听 `127.0.0.1:6379`。

### 3. 启动后端

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

启动成功后，API 地址为：

```text
http://localhost:22224/api
```

### 4. 启动前端

另开一个终端：

```powershell
cd frontend
.\mvnw.cmd javafx:run
```

也可以在 IntelliJ IDEA 中运行：

```text
com.tihu.frontend.MainApplication
```

## 前端后端地址

前端默认连接本机后端：

```text
http://localhost:22224/api
```

如果后端部署在其他电脑，可以覆盖地址：

```powershell
$env:TIHU_BACKEND_BASE_URL="http://服务器地址:22224/api"
.\mvnw.cmd javafx:run
```

也可以使用 JVM 参数：

```powershell
.\mvnw.cmd javafx:run -Dtihu.backend.base-url=http://服务器地址:22224/api
```

联调和验收时建议开启严格远程模式，避免接口失败后回退到本地 Mock：

```powershell
$env:TIHU_REMOTE_STRICT="true"
.\mvnw.cmd javafx:run
```

## 核心功能

| 模块 | 功能 |
| --- | --- |
| 用户认证 | 普通注册、管理员邀请码注册、登录、登出、Token 鉴权、封禁登录提示 |
| 首页 | 项目简介、团队贡献展示、日间/夜间主题 |
| 图书浏览 | 图书列表、分页、搜索、标签筛选、评分排序、封面展示 |
| 图书详情 | 简介、封面、标签、评分统计、我的评分、收藏状态和评论区 |
| 评分评论 | 1 到 10 分评分，评论、回复、删除、点赞、点踩、取消投票 |
| 收藏 | 收藏图书、取消收藏、收藏列表、收藏状态检查 |
| 书单 | 创建书单、公开/私密切换、添加/移除图书、删除书单 |
| 用户主页 | 用户资料、评论、公开书单、关注和取关 |
| 关注粉丝 | 关注列表、粉丝列表、用户关系查询 |
| 私信 | 会话列表、一对一聊天、按用户名发起私信 |
| 管理后台 | 图书管理、用户管理、封禁/解封、授权管理员、评论管理 |

## 数据库说明

老师提供的远程数据库 `java_3_14` 已导入本项目原始数据。当前验证结果：

```text
表数量：14
用户数量：14
图书数量：80
```

`dist/tihu-database.sql` 只用于在已有 `java_3_14` schema 中恢复表和数据，不包含 `CREATE DATABASE`。不要把它导入到其他团队库。

## 测试

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

## 相关文档

| 文档 | 用途 |
| --- | --- |
| `backend/README.md` | 后端配置、启动、接口约定和排错 |
| `frontend/README.md` | 前端运行、后端地址覆盖、Mock 模式和交互规则 |
| `frontend/docs/frontend-api.md` | 前端实际调用的 API 路径、字段和响应约定 |
| `dist/README.md` | 提交包或分发包的运行说明 |

## 常见问题

### 后端启动失败

检查 JDK、Redis、远程 MySQL 网络连接和 `application-dev.yaml`。数据库地址应为 `202.194.14.120:3306/java_3_14`。

### 前端登录失败

确认后端已启动在 `http://localhost:22224/api`。如果正在联调真实后端，开启 `TIHU_REMOTE_STRICT=true`，避免 Mock 回退掩盖问题。

### 看不到远程数据

确认前端实际连接的是 `localhost:22224` 的后端，后端实际连接的是 `202.194.14.120:3306/java_3_14`。

### Redis 连接失败

启动本机 Redis，或按实际环境修改 `backend/src/main/resources/application-dev.yaml` 中的 Redis 配置。

## 文档维护

功能、接口或配置变化后，优先同步：

1. `frontend/docs/frontend-api.md`
2. `README.md`
3. `backend/README.md`
4. `frontend/README.md`
5. `dist/README.md`
6. `dist/TiHu-submit.zip`
