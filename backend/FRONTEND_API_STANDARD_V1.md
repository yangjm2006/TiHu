# TiHu 后端 API 接口标准（前端自查版）

> 适用对象：前端 JavaFX 开发者
>
> 目标：帮助前端快速自查“为什么登录不上 / 为什么接口 500 / 为什么分页不显示 / 为什么 Cookie 丢失”等对接问题。
>
> 说明：本文以**当前后端实际实现**为准，优先保证可对接、可运行、可排错。

---

## 1. 最重要的 6 个约定

前端先确认下面 6 项，绝大多数问题都能在这里定位：

1. **基址固定**：
   - `http://localhost:9090/api`
2. **路径前缀固定**：
   - 所有接口都以 `/api` 开头
3. **响应格式固定**：
   - `code / message / data`
4. **登录态固定**：
   - 使用 `Session + Cookie`
5. **分页格式固定**：
   - `records / total / pages / current / size`
6. **请求体字段固定**：
   - 登录/注册使用 `username`、`password`

---

## 2. 基址与路径规范

### 2.1 正确的前端基址

前端请求应使用：

```text
http://localhost:9090/api
```

示例：

- `POST http://localhost:9090/api/users/login`
- `GET http://localhost:9090/api/books/1`

### 2.2 常见错误

#### 错误 1：重复拼接 `/api`

```text
❌ http://localhost:9090/api/api/users/login
```

正确：

```text
✅ http://localhost:9090/api/users/login
```

#### 错误 2：少写 `/api`

```text
❌ http://localhost:9090/users/login
```

正确：

```text
✅ http://localhost:9090/api/users/login
```

---

## 3. 统一响应格式

### 3.1 成功响应

```json
{
  "code": 200,
  "message": "OK",
  "data": {}
}
```

### 3.2 失败响应

```json
{
  "code": 400,
  "message": "用户名不能为空",
  "data": null
}
```

### 3.3 前端注意事项

- 不要再读 `msg`
- 正确字段是 `message`
- `code == 200` 表示成功

### 3.4 常见字段误区

```javascript
// 错误
const text = response.msg;

// 正确
const text = response.message;
```

### 3.5 前端实现敏感点（务必自查）

前端同学请特别注意下面 4 点，这些是联调时最容易出问题的地方：

#### 1）前端只认 `code = 200`

- 只有 `code == 200` 才当作成功
- 不要把 `0 / 201 / 204` 当作成功码
- 其他 code 前端会按失败处理

#### 2）Cookie 最好只保存一个主 Session

- 前端 `ApiClient` 目前只保留第一个 `Set-Cookie`
- 如果后端一次返回多个 Cookie，前端可能只记住第一个
- 建议后端登录态保持一个核心 Session Cookie 即可

#### 3）分页接口不要把 `size` 卡死

- 前端某些页面会请求较大的分页大小
- 图书列表、收藏列表、书单列表、评论列表都可能传较大的 `size`
- 后端应直接接受前端传入的 `size`，不要强行限制得太小
- 分页结果前端按 `records` 解析

#### 4）路径参数尽量保持简单稳定

- 前端对路径段编码只做了基础处理
- 用户名路径参数可以用，但最好避免特殊字符导致路由问题
- 后端路径设计应尽量稳定、简单

#### 5）时间字段必须是 ISO-8601 字符串

- 不要返回时间戳数字
- 统一使用如 `2026-05-20T13:45:00` 这样的格式

#### 6）登录成功后必须有 `Set-Cookie`

- 登录接口成功后必须返回 session cookie
- 前端会保存并在后续请求中携带
- 没有 cookie，后续接口很容易返回 `401`

---

## 4. 分页响应格式

所有分页接口都统一返回：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [],
    "total": 0,
    "pages": 0,
    "current": 1,
    "size": 10
  }
}
```

### 4.1 前端必须这样处理

```javascript
const { records, total, pages, current, size } = response.data;
```

### 4.2 不要这样处理

```javascript
// 错误：response.data 是整个分页对象，不是列表本身
const list = response.data;
```

---

## 5. 登录态与 Cookie 规范

### 5.1 登录方式

后端使用：

- `Session + Cookie`
- Sa-Token 负责登录态管理

### 5.2 登录成功后前端必须做的事

1. 读取后端返回的 `Set-Cookie`
2. 保存登录态
3. 后续请求携带 Cookie

### 5.3 前端自查点

- 登录后浏览器/客户端是否保存了 Cookie
- 后续请求头里是否带了 Cookie
- Session 是否过期
- 是否跨域导致 Cookie 没被保存

### 5.4 登录失败常见表现

| 现象 | 可能原因 |
|---|---|
| 返回 500 | 请求字段缺失、后端空指针、数据库异常 |
| 返回 401 | 用户名或密码错误、Session 失效 |
| 返回 403 | 被封禁、无权限 |

---

## 6. 请求格式规范

### 6.1 登录 / 注册统一使用 JSON

前端应以 JSON 发请求体：

```json
{
  "username": "alice",
  "password": "Alice123"
}
```

### 6.2 注册管理员

管理员注册时，邀请码通过 Query 参数传递：

```text
POST /api/users/register?inviteCode=123456
```

请求体仍然是：

```json
{
  "username": "admin2",
  "password": "Admin123"
}
```

### 6.3 常见字段名

| 功能 | 必填字段 |
|---|---|
| 注册 / 登录 | `username`, `password` |
| 书籍分页 | `page`, `size` |
| 评分 | `bookId`, `score` |
| 评论 | `bookId`, `content`, `parentCommentId` |
| 收藏 | `bookId` |
| 书单 | `title`, `description` |
| 关注 | `followeeId` |
| 私信 | `receiverId`, `content` |

---

## 7. 关键接口总览

> 下面这些接口是前端最容易出错的，请优先自查。

### 7.1 用户接口

#### 1）注册普通用户

```text
POST /api/users/register
```

请求体：

```json
{
  "username": "alice",
  "password": "Alice123"
}
```

#### 2）登录

```text
POST /api/users/login
```

请求体：

```json
{
  "username": "alice",
  "password": "Alice123"
}
```

#### 3）退出登录

```text
POST /api/users/logout
```

#### 4）获取当前用户

```text
GET /api/users/me
```

#### 5）按 ID 获取用户

```text
GET /api/users/{id}
```

#### 6）用户主页

```text
GET /api/users/profile/{username}
```

---

### 7.2 图书接口

#### 1）图书分页列表

```text
GET /api/books?page=1&size=10
```

#### 2）图书详情

```text
GET /api/books/{id}
```

#### 3）新增图书（管理员）

```text
POST /api/books
```

#### 4）编辑图书（管理员）

```text
PUT /api/books/{id}
```

#### 5）删除图书（管理员）

```text
DELETE /api/books/{id}
```

---

### 7.3 评分接口

#### 1）提交 / 更新评分

```text
POST /api/ratings?bookId=1&score=8
```

#### 2）获取我的评分

```text
GET /api/ratings/my?bookId=1
```

#### 3）获取评分统计

```text
GET /api/ratings/book/{bookId}/stats
```

---

### 7.4 评论接口

#### 1）发表评论 / 回复

```text
POST /api/comments?bookId=1&content=很好看&parentCommentId=1001
```

#### 2）获取图书评论

```text
GET /api/comments/book/{bookId}?page=1&size=10
```

#### 3）删除自己的评论

```text
DELETE /api/comments/{commentId}
```

#### 4）管理员删除评论

```text
DELETE /api/comments/admin/{commentId}
```

#### 5）管理员查看全站评论

```text
GET /api/comments/admin/all
```

---

### 7.5 收藏接口

#### 1）收藏图书

```text
POST /api/collections?bookId=1
```

#### 2）取消收藏

```text
DELETE /api/collections?bookId=1
```

#### 3）检查是否收藏

```text
GET /api/collections/check?bookId=1
```

#### 4）我的收藏列表

```text
GET /api/collections?page=1&size=10
```

---

### 7.6 书单接口

#### 1）创建书单

```text
POST /api/book-lists?title=我的书单&description=简介
```

#### 2）我的书单列表

```text
GET /api/book-lists?page=1&size=10
```

#### 3）书单详情

```text
GET /api/book-lists/{listId}
```

#### 4）向书单加书

```text
POST /api/book-lists/{listId}/books?bookId=1
```

#### 5）从书单移除图书

```text
DELETE /api/book-lists/{listId}/books?bookId=1
```

#### 6）删除书单

```text
DELETE /api/book-lists/{listId}
```

---

### 7.7 关注接口

#### 1）关注用户

```text
POST /api/follows?followeeId=3
```

#### 2）取消关注

```text
DELETE /api/follows?followeeId=3
```

#### 3）我关注的人

```text
GET /api/follows/followees?page=1&size=10
```

#### 4）我的粉丝

```text
GET /api/follows/followers?page=1&size=10
```

#### 5）检查是否关注

```text
GET /api/follows/check?followeeId=3
```

---

### 7.8 私信接口

#### 1）发送私信

```text
POST /api/messages?receiverId=3&content=你好
```

#### 2）会话列表

```text
GET /api/messages/conversations
```

#### 3）某会话全部历史消息

```text
GET /api/messages/conversation/{peerId}?page=1&size=100
```

---

### 7.9 管理员接口

#### 1）获取封禁列表

```text
GET /api/users/admin/bans
```

#### 2）封禁用户

```text
POST /api/users/admin/{id}/ban?durationSeconds=86400
```

#### 3）解封用户

```text
DELETE /api/users/admin/{id}/ban
```

---

## 8. 常见错误码与对应原因

| code | 含义 | 前端排查方向 |
|---|---|---|
| 200 | 成功 | 正常处理 |
| 400 | 参数错误 | 请求字段缺失、字段名不对、格式不对 |
| 401 | 未登录 / 登录失败 | Cookie 丢失、密码错误、Session 过期 |
| 403 | 无权限 | 被封禁、普通用户调用管理员接口 |
| 404 | 资源不存在 | 用户、图书、评论、书单不存在 |
| 409 | 冲突 | 用户名重复、书名重复、重复关注、重复收藏 |
| 500 | 服务器内部错误 | 数据库异常、空指针、代码 bug |

---

## 9. 前端自查清单

### 9.1 登录前先检查
- [ ] 请求地址是否是 `http://localhost:9090/api/users/login`
- [ ] 请求体是否为 JSON
- [ ] 字段名是否是 `username` / `password`
- [ ] 是否带了正确的 `Content-Type: application/json`

### 9.2 登录后检查
- [ ] 是否收到了 `Set-Cookie`
- [ ] Cookie 是否被保存
- [ ] 后续请求是否携带了 Cookie
- [ ] 是否把 `Authorization` 当成唯一登录态来源（不建议）

### 9.3 分页接口检查
- [ ] 是否从 `response.data.records` 取列表
- [ ] 是否读取了 `total / pages / current / size`
- [ ] 是否把 `page` 从 1 开始传

### 9.4 用户主页检查
- [ ] 是否展示 `userInfo`
- [ ] 是否展示 `comments`
- [ ] 是否展示 `bookLists`
- [ ] 是否展示 `followingCount / followerCount`
- [ ] 是否处理 `followedByCurrentUser`

### 9.5 管理员接口检查
- [ ] 当前登录用户是否是 ADMIN
- [ ] 是否能正常访问 `/admin/**` 路径
- [ ] 是否正确处理 403

---

## 10. 常见问题与答案

### Q1：登录为什么显示 500？

优先检查：

1. 请求体是否为空
2. `username/password` 字段是否写错
3. Cookie 是否正确保存
4. 数据库是否正常连接

### Q2：为什么后端说密码为空？

说明前端没有把 `password` 传到后端，或者字段名不对。

正确请求示例：

```json
{
  "username": "alice",
  "password": "Alice123"
}
```

### Q3：为什么分页列表拿不到数据？

常见原因：

- 前端用了 `response.data`，但没读 `records`
- `page` 从 0 开始传了
- Cookie 丢失导致请求被拦截

### Q4：为什么接口返回 401？

常见原因：

- 没登录
- 登录态过期
- Cookie 没带上
- 前端请求跨域时没有正确保存 Cookie

### Q5：为什么接口返回 403？

常见原因：

- 调用了管理员接口，但当前用户不是 ADMIN
- 用户被封禁

---

## 11. 前端推荐的处理方式

### 11.1 统一解析响应

```javascript
if (response.code === 200) {
    // success
} else {
    alert(response.message);
}
```

### 11.2 统一处理分页

```javascript
const { records, total, pages, current, size } = response.data;
```

### 11.3 统一处理登录失效

```javascript
if (response.code === 401) {
    alert('登录已失效，请重新登录');
    // 跳转登录页
}
```

---

## 12. 最后强调一次

### 前端最容易错的 4 件事

1. **把 `msg` 写成了 `message` 以外的字段**
2. **请求路径多写了一个 `/api`**
3. **登录后 Cookie 没保存 / 没携带**
4. **注册/登录请求体字段名写错**

### 正确示例

```text
POST http://localhost:9090/api/users/login
```

```json
{
  "username": "alice",
  "password": "Alice123"
}
```

---

**文档用途**：前端自查、联调排错、接口对齐

**当前后端约定版本**：V1

**建议前端优先阅读顺序**：
1. 第 1 ~ 5 节
2. 第 7 节接口总览
3. 第 9 节自查清单
4. 第 10 节常见问题

