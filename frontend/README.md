# TiHu 前端客户端

前端是 TiHu 的 JavaFX 桌面客户端，负责页面展示、用户交互和调用后端 REST API。默认连接本机后端 `http://localhost:22224/api`，也保留本地 Mock 服务用于离线演示和测试。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 运行环境 | Java 21 |
| UI | JavaFX 21.0.6, FXML, Scene Builder |
| HTTP | Java HttpClient |
| JSON | Jackson |
| 构建 | Maven Wrapper |
| 测试 | JUnit 5, FXML 加载测试, API 合同测试 |

## 目录结构

```text
frontend/
├── pom.xml
├── mvnw / mvnw.cmd
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

## 后端地址

默认地址：

```text
http://localhost:22224/api
```

该地址指向本机 Spring Boot 后端。后端再连接老师提供的远程 MySQL `202.194.14.120:3306/java_3_14`。

如需连接其他后端，可使用环境变量：

```powershell
$env:TIHU_BACKEND_BASE_URL="http://服务器地址:22224/api"
.\mvnw.cmd javafx:run
```

或 JVM 参数：

```powershell
.\mvnw.cmd javafx:run -Dtihu.backend.base-url=http://服务器地址:22224/api
```

## 启动前检查

1. JDK 21 可用。
2. 后端已启动在 `http://localhost:22224/api`。
3. 联调真实后端时建议开启严格远程模式。

严格远程模式：

```powershell
$env:TIHU_REMOTE_STRICT="true"
.\mvnw.cmd javafx:run
```

未开启严格模式时，部分网络异常会回退到本地 Mock，便于离线演示，但也可能掩盖后端接口问题。

## 启动客户端

Windows PowerShell：

```powershell
.\mvnw.cmd javafx:run
```

macOS/Linux：

```bash
chmod +x mvnw
./mvnw javafx:run
```

IntelliJ IDEA 运行入口：

```text
com.tihu.frontend.MainApplication
```

## 功能清单

| 模块 | 功能 |
| --- | --- |
| 登录注册 | 普通注册、管理员邀请码注册、登录鉴权、封禁登录提示 |
| 首页 | 项目简介、团队贡献展示、日间/夜间主题 |
| 图书 | 图书列表、搜索、标签筛选、评分排序、分页展示 |
| 图书详情 | 图书信息、封面、标签、评分统计、我的评分、收藏状态 |
| 评论 | 发表、回复、删除、点赞、点踩、取消投票 |
| 收藏 | 收藏列表、搜索收藏、分页、取消收藏 |
| 书单 | 创建、公开/私密、添加/移除图书、删除 |
| 用户主页 | 用户信息、评论、公开书单、关注/取关 |
| 关注粉丝 | 关注列表、粉丝列表、关系查看 |
| 私信 | 会话列表、一对一聊天、按用户名发起会话 |
| 管理后台 | 图书管理、用户管理、封禁/解封、授权管理员、评论管理 |

## Mock 模式账号

仅本地 Mock 模式可用：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Admin123` | 管理员 |
| `alice` | `Alice123` | 普通用户 |
| `bob` | `Bob12345` | 普通用户 |

连接真实后端时，请以远程数据库中的实际账号为准，也可以通过注册页面新建账号。

管理员注册邀请码：

```text
123456
```

## 交互和数据规则

- 所有页面展示日期统一为 `yyyy-MM-dd HH:mm:ss`。
- 图书列表每页展示 9 本书。
- 收藏页面每页展示 9 本书。
- 我的书单按完整书名添加图书，找不到时提示“书名不存在”。
- 管理员用户管理页按注册时间从旧到新排序。
- 用户解封后，后端必须返回空封禁时间，前端显示“正常”。
- 图片字段支持 URL 或 `data:image/...;base64,...`。

## API 文档

前端实际调用接口以此文档为准：

```text
docs/frontend-api.md
```

文档包含：

- 通用请求、响应、分页和时间字段约定
- 用户、图书、评分、评论、收藏、书单、用户主页、关注、私信、管理员接口
- 前端兼容字段说明
- 前端实际调用接口清单

## 测试

```powershell
.\mvnw.cmd test
```

清理重编译：

```powershell
.\mvnw.cmd clean compile
```

macOS/Linux 将 `.\mvnw.cmd` 替换为 `./mvnw`。

## 常见问题

### 前端登录失败

确认后端已启动在 `http://localhost:22224/api`，并确认账号属于远程数据库当前数据。

### 页面数据和远程数据库不一致

开启 `TIHU_REMOTE_STRICT=true` 后重试，确认请求没有回退到 Mock。

### 接口字段解析失败

以后端返回是否符合 `code=200 + data` 为优先检查项，再对照 `docs/frontend-api.md`。

### 图片或头像不显示

确认后端返回的是可访问 URL、空值，或合法的 `data:image/...;base64,...`。
