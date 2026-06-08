# TiHu 后端服务

后端是 TiHu 的 REST API 服务，负责登录鉴权、用户资料、图书、评分、评论、收藏、书单、关注、私信和管理员后台能力。前端默认通过 `http://localhost:22224/api` 调用本服务。

## 运行配置

| 配置项 | 当前值 |
| --- | --- |
| Spring Profile | `dev` |
| 服务端口 | `22224` |
| Context Path | `/` |
| MySQL URL | `jdbc:mysql://202.194.14.120:3306/java_3_14` |
| MySQL 用户 | `java_3_14` |
| MySQL 密码 | `JavaP314@` |
| Redis | `127.0.0.1:6379` |

配置文件：

```text
src/main/resources/application.yaml
src/main/resources/application-dev.yaml
src/main/resources/application-prod.yaml
```

`dev` 和 `prod` 均已配置为远程 MySQL `202.194.14.120:3306/java_3_14` 和后端端口 `22224`。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 运行环境 | Java 21 |
| Web 框架 | Spring Boot 4 |
| ORM | MyBatis-Plus |
| 鉴权 | Sa-Token |
| 数据库 | 远程 MySQL |
| 缓存 | Redis |
| 构建 | Maven Wrapper |

## 目录结构

```text
backend/
├── pom.xml
├── mvnw / mvnw.cmd
├── schema.sql
├── start.bat
├── start.sh
├── src/main/java/com/tihu/backend/
├── src/main/resources/
│   ├── application.yaml
│   ├── application-dev.yaml
│   ├── application-prod.yaml
│   └── schema.sql
└── README.md
```

## 启动前检查

1. JDK 21 可用。
2. 本机 Redis 已启动。
3. 远程数据库可连接。

远程数据库检查：

```powershell
mysql -h 202.194.14.120 -P 3306 -u java_3_14 -pJavaP314@ java_3_14 -e "SHOW TABLES;"
```

如果本机没有 `mysql` 命令，可以用 MySQL Workbench 连接：

```text
Host: 202.194.14.120
Port: 3306
Username: java_3_14
Password: JavaP314@
Default Schema: java_3_14
```

## 启动服务

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

macOS/Linux：

```bash
chmod +x mvnw
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

启动成功后访问：

```text
http://localhost:22224/api
```

## 打包运行

打包：

```powershell
.\mvnw.cmd clean package -DskipTests
```

运行 jar：

```powershell
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

## 数据库脚本

项目包含两份同内容脚本：

```text
schema.sql
src/main/resources/schema.sql
```

`application-dev.yaml` 中已开启：

```yaml
spring:
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
```

后端启动时会尝试初始化表结构。远程 `java_3_14` 已导入项目数据，正常运行不需要手动重新导入。

## API 模块

| 模块 | 能力 |
| --- | --- |
| 用户认证 | 注册、管理员邀请码注册、登录、登出、当前用户信息 |
| 用户资料 | 修改用户名、密码、头像，按用户名查询主页 |
| 图书 | 图书列表、搜索、详情、新增、编辑、删除、标签 |
| 评分 | 评分、我的评分、评分统计 |
| 评论 | 发表、回复、删除、点赞、点踩、管理员评论管理 |
| 收藏 | 收藏、取消收藏、收藏列表、收藏状态检查 |
| 书单 | 创建、公开/私密、添加/移除图书、删除 |
| 关注 | 关注、取消关注、关注列表、粉丝列表 |
| 私信 | 发送私信、会话列表、会话消息 |
| 管理员 | 用户管理、封禁/解封、授予管理员权限 |

## 接口约定

- 成功响应使用 `code=200`。
- 业务数据放在 `data`。
- 错误信息放在 `message`。
- 登录成功后返回 `Authorization`，前端后续请求自动携带。
- 时间字段推荐 ISO-8601 本地时间，例如 `2026-06-04T15:00:00`。
- 图书封面和用户头像支持 URL 或 `data:image/...;base64,...`。
- 解封用户时必须清空 `ban_expire_time`。

前后端接口以客户端文档为准：

```text
../frontend/docs/frontend-api.md
```

## 测试

```powershell
.\mvnw.cmd test
```

macOS/Linux：

```bash
./mvnw test
```

## 常见问题

### 数据库连接失败

确认网络可访问 `202.194.14.120:3306`，账号为 `java_3_14`，密码为 `JavaP314@`。

### Redis 连接失败

启动本机 Redis，或修改 `application-dev.yaml` 中的 Redis 地址和密码。

### 前端接口失败

确认后端监听 `22224`，并且接口路径包含 `/api` 前缀。

### 登录数据不对

当前运行数据来自远程 `java_3_14`。如果你只改了本机 MySQL，后端不会读取那些数据。
