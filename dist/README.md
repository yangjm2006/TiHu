# TiHu 鹈鹕图书分享交流平台

这是 TiHu 项目的分发说明。TiHu 是一个基于 JavaFX + Spring Boot 的桌面端图书分享交流平台，包含前端客户端和后端 API 服务，支持图书浏览、评分评论、收藏书单、关注粉丝、私信会话和管理员后台管理。

本说明不依赖任何固定电脑路径。把项目解压或克隆到任意目录后，从包含 `backend`、`frontend`、`README.md` 的项目根目录开始操作即可。

## 环境要求

请先安装：

- JDK 21 或以上
- MySQL 8 或兼容版本
- Redis
- Maven 可选；项目已包含 Maven Wrapper，Windows 使用 `mvnw.cmd`，macOS/Linux 使用 `./mvnw`

后端默认配置：

```text
MySQL: localhost:3306/tihu
MySQL 用户名/密码: root/root
Redis: 127.0.0.1:6379
API:   http://localhost:9090/api
```

如果目标电脑的数据库或 Redis 配置不同，请修改：

```text
backend/src/main/resources/application-dev.yaml
```

## 项目结构

```text
TiHu/
├── backend/
│   ├── mvnw / mvnw.cmd
│   ├── pom.xml
│   ├── schema.sql
│   └── src/main/resources/application-dev.yaml
├── frontend/
│   ├── mvnw / mvnw.cmd
│   ├── pom.xml
│   └── docs/frontend-api.md
├── README.md
└── dist/
    └── README.md
```

## 启动步骤

### 1. 初始化数据库

进入后端目录，创建数据库并导入脚本。

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

如果 `mysql` 命令不可用，请把 MySQL 的 `bin` 目录加入 PATH，或使用图形化数据库工具执行 `backend/schema.sql`。

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

启动成功后访问地址为：

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

也可以在 IntelliJ IDEA 中运行：

```text
com.tihu.frontend.MainApplication
```

## 前端连接其他后端

前端默认连接：

```text
http://localhost:9090/api
```

如果后端部署在其他电脑或端口，可以设置环境变量。

Windows PowerShell：

```powershell
$env:TIHU_BACKEND_BASE_URL="http://服务器地址:9090/api"
.\mvnw.cmd javafx:run
```

macOS/Linux：

```bash
TIHU_BACKEND_BASE_URL="http://服务器地址:9090/api" ./mvnw javafx:run
```

联调后端时建议开启严格远程模式，避免接口失败后回退到本地 Mock。

Windows PowerShell：

```powershell
$env:TIHU_REMOTE_STRICT="true"
.\mvnw.cmd javafx:run
```

macOS/Linux：

```bash
TIHU_REMOTE_STRICT="true" ./mvnw javafx:run
```

## Mock 测试账号

后端不可用或前端回退到 Mock 模式时，可使用：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Admin123` | 管理员 |
| `alice` | `Alice123` | 普通用户 |
| `bob` | `Bob12345` | 普通用户 |

管理员注册邀请码：

```text
123456
```

连接真实后端时，请以实际数据库用户为准。

## 测试命令

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

检查 MySQL 和 Redis 是否已启动，并确认 `backend/src/main/resources/application-dev.yaml` 中账号、密码、端口正确。

### 前端登录后没有远程数据

确认后端是否监听在 `9090` 端口，接口是否带 `/api` 前缀。联调时建议开启 `TIHU_REMOTE_STRICT=true`。

### 图片或头像不显示

后端应返回可直接展示的图片 URL 或 `data:image/...;base64,...`。清空封面或头像时返回空字符串或 `null`。

## 更多文档

- 项目总说明：`../README.md`
- 前端说明：`../frontend/README.md`
- 后端说明：`../backend/README.md`
- API 文档：`../frontend/docs/frontend-api.md`
