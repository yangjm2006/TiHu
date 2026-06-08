# TiHu 分发包说明

本目录用于课程提交和快速运行，包含后端 jar、前端 jar、数据库恢复脚本和提交压缩包。

## 文件说明

| 文件 | 说明 |
| --- | --- |
| `backend-0.0.1-SNAPSHOT.jar` | 后端 Spring Boot 可执行 jar |
| `frontend-1.0-SNAPSHOT-all.jar` | 前端 JavaFX 可执行 jar |
| `tihu-database.sql` | 在已有 `java_3_14` schema 中恢复表和数据 |
| `TiHu-submit.zip` | 课程提交压缩包 |
| `README.md` | 当前分发说明 |

## 当前配置

| 配置项 | 当前值 |
| --- | --- |
| 后端端口 | `22224` |
| API 地址 | `http://localhost:22224/api` |
| MySQL 主机 | `202.194.14.120:3306` |
| MySQL 数据库 | `java_3_14` |
| MySQL 用户名 | `java_3_14` |
| MySQL 密码 | `JavaP314@` |
| Redis | `127.0.0.1:6379` |

MySQL 是老师提供的远程数据库。前端仍然访问本机后端，后端再访问远程 MySQL。

## 环境要求

- JDK 21 或以上
- 本机 Redis
- 可访问 `202.194.14.120:3306` 的网络环境
- MySQL 客户端或 MySQL Workbench 可选，用于检查远程库

## 检查远程数据库

```powershell
mysql -h 202.194.14.120 -P 3306 -u java_3_14 -pJavaP314@ java_3_14 -e "SHOW TABLES;"
```

如果需要恢复数据库数据：

```powershell
mysql -h 202.194.14.120 -P 3306 -u java_3_14 -pJavaP314@ java_3_14 < tihu-database.sql
```

注意：`tihu-database.sql` 不创建数据库，只在已有 `java_3_14` 中建表和导入数据。

## 启动后端

先启动本机 Redis，然后运行：

```powershell
java -jar backend-0.0.1-SNAPSHOT.jar
```

启动成功后 API 地址：

```text
http://localhost:22224/api
```

## 启动前端

```powershell
java -jar frontend-1.0-SNAPSHOT-all.jar
```

如果要连接其他后端：

```powershell
$env:TIHU_BACKEND_BASE_URL="http://服务器地址:22224/api"
java -jar frontend-1.0-SNAPSHOT-all.jar
```

联调时建议开启严格远程模式：

```powershell
$env:TIHU_REMOTE_STRICT="true"
java -jar frontend-1.0-SNAPSHOT-all.jar
```

## 运行顺序

1. 确认网络可访问远程 MySQL。
2. 启动本机 Redis。
3. 启动后端 jar。
4. 启动前端 jar。
5. 使用远程数据库中的账号登录，或在前端注册新账号。

## 常见问题

### 后端启动失败

检查 JDK、Redis 和远程 MySQL 连接。后端应监听 `22224`。

### 前端无法登录

确认后端已经启动，并访问的是 `http://localhost:22224/api`。真实后端账号以远程 `java_3_14` 中的数据为准。

### 数据库恢复失败

确认使用的是 `java_3_14` 账号，并且目标 schema 是 `java_3_14`。不要把脚本导入其他团队数据库。

### 数据和演示不一致

确认前端没有回退到 Mock。联调时设置：

```powershell
$env:TIHU_REMOTE_STRICT="true"
```
