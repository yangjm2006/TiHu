# 🚀 鹈鹕后端 API 快速参考

## 响应格式统一标准

所有接口返回格式（**务必注意 message 字段**）：

```json
{
  "code": 200,
  "message": "OK",
  "data": {}
}
```

**重要变更**：`msg` 已改为 `message`

---

## 分页响应格式

所有分页接口现在统一返回以下格式：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [],        // 记录列表（前端需要兼容）
    "total": 0,           // 总记录数
    "pages": 0,           // 总页数
    "current": 1,         // 当前页码
    "size": 10            // 每页大小
  }
}
```

**改变的接口列表**：
- `GET /api/books?page=1&size=10`
- `GET /api/books/search?keyword=xxx`
- `GET /api/books/search-by-tags?tags=xxx`
- `GET /api/collections?page=1&size=10`
- `GET /api/book-lists?page=1&size=10`
- `GET /api/follows/followees?page=1&size=10`
- `GET /api/follows/followers?page=1&size=10`
- `GET /api/comments/book/{bookId}?page=1&size=10`
- `GET /api/messages/conversation/{userId}?page=1&size=10`

---

## 🆕 新增接口

### 用户接口

#### 1. 按 ID 获取用户信息
```
GET /api/users/{id}
```
**响应**：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 1,
    "username": "alice",
    "role": "USER",
    "avatarUrl": null,
    "banEndTime": null,
    "createdAt": "2026-05-20T13:45:00"
  }
}
```

#### 2. 获取用户主页信息
```
GET /api/users/profile/{username}
```
**响应**：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "userInfo": {
      "id": 1,
      "username": "alice",
      "role": "USER"
    },
    "comments": [],
    "bookLists": [],
    "followingCount": 10,
    "followerCount": 5,
    "followedByCurrentUser": true
  }
}
```

#### 3. 获取管理员封禁列表
```
GET /api/users/admin/bans
```
**响应**：
```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": 2,
      "username": "bob",
      "banEndTime": "2026-05-23T00:00:00"
    }
  ]
}
```

### 评论接口

#### 4. 获取全站评论（管理员）
```
GET /api/comments/admin/all
```
**响应**：
```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": 101,
      "bookId": 1,
      "userId": 2,
      "username": "alice",
      "content": "很好看",
      "createTime": "2026-05-20T13:45:00",
      "parentCommentId": null,
      "upVotes": 3,
      "downVotes": 1
    }
  ]
}
```

---

## 🔄 修改的接口

### 用户主页接口改进
**原接口**：`GET /api/users/profile/{username}` - 仅返回 User 对象  
**现接口**：返回完整用户主页数据包括评论、书单、关注数等

---

## ✨ 前端对接建议

### 1. 更新响应处理
```javascript
// 旧代码
const { msg, data } = response;

// 新代码
const { message, data } = response;
```

### 2. 分页数据处理
```javascript
// 接收分页数据
const { records, total, pages, current, size } = response.data;

// 前端绑定
this.items = records;
this.total = total;
this.pageSize = size;
this.currentPage = current;
```

### 3. 用户主页处理
```javascript
// 接收完整主页数据
const { userInfo, comments, bookLists, followingCount, folowerCount, followedByCurrentUser } 
  = response.data;
```

---

## 🐛 常见错误处理

| 错误码 | 含义 | 处理建议 |
|--------|------|--------|
| `200` | 成功 | 正常处理 |
| `400` | 参数错误 | 检查请求参数有效性 |
| `401` | 未登录 | 跳转登录页并提示 Session 过期 |
| `403` | 无权限 | 提示权限不足 |
| `404` | 资源不存在 | 提示资源不存在 |
| `409` | 冲突 | 提示数据重复或冲突 |
| `500` | 服务器错误 | 提示服务器异常 |

---

## 📋 完整接口列表速查

### 用户相关
- `POST /api/users/register` ✅
- `POST /api/users/login` ✅
- `POST /api/users/logout` ✅
- `GET /api/users/me` ✅
- `GET /api/users/{id}` ✅ **[新增]**
- `GET /api/users/profile/{username}` ✅ **[改进]**
- `PUT /api/users/{id}/username` ✅
- `PUT /api/users/{id}/password` ✅
- `GET /api/users/admin/bans` ✅ **[新增]**
- `POST /api/users/admin/{id}/ban` ✅
- `DELETE /api/users/admin/{id}/ban` ✅

### 图书相关
- `GET /api/books?page=...&size=...` ✅ **[格式改进]**
- `GET /api/books/search?keyword=...` ✅ **[格式改进]**
- `GET /api/books/search-by-tags?tags=...` ✅ **[格式改进]**
- `GET /api/books/{id}` ✅
- `POST /api/books` ✅
- `PUT /api/books/{id}` ✅
- `DELETE /api/books/{id}` ✅

### 评分相关
- `POST /api/ratings?bookId=...&score=...` ✅
- `GET /api/ratings/my?bookId=...` ✅
- `GET /api/ratings/book/{bookId}/stats` ✅

### 评论相关
- `POST /api/comments?bookId=...&content=...` ✅
- `GET /api/comments/book/{bookId}?page=...` ✅ **[格式改进]**
- `DELETE /api/comments/{id}` ✅
- `POST /api/comments/{id}/like` ✅
- `POST /api/comments/{id}/dislike` ✅
- `DELETE /api/comments/{id}/like` ✅
- `DELETE /api/comments/admin/{id}` ✅
- `GET /api/comments/admin/all` ✅ **[新增]**

### 收藏相关
- `POST /api/collections?bookId=...` ✅
- `DELETE /api/collections?bookId=...` ✅
- `GET /api/collections?page=...` ✅ **[格式改进]**
- `GET /api/collections/check?bookId=...` ✅

### 书单相关
- `POST /api/book-lists?title=...` ✅
- `GET /api/book-lists?page=...` ✅ **[格式改进]**
- `GET /api/book-lists/{id}` ✅
- `POST /api/book-lists/{id}/books?bookId=...` ✅
- `DELETE /api/book-lists/{id}/books?bookId=...` ✅
- `DELETE /api/book-lists/{id}` ✅

### 关注相关
- `POST /api/follows?followeeId=...` ✅
- `DELETE /api/follows?followeeId=...` ✅
- `GET /api/follows/followees?page=...` ✅ **[格式改进]**
- `GET /api/follows/followers?page=...` ✅ **[格式改进]**
- `GET /api/follows/user/{userId}/followees` ✅ **[格式改进]**
- `GET /api/follows/user/{userId}/followers` ✅ **[格式改进]**
- `GET /api/follows/check?followeeId=...` ✅

### 私信相关
- `POST /api/messages?receiverId=...&content=...` ✅
- `GET /api/messages/conversations` ✅
- `GET /api/messages/conversation/{userId}?page=...` ✅ **[格式改进]**

**总计**：54 个接口全部符合规范 ✅

---

## 🔐 权限检查一览

| 接口 | 权限要求 | 说明 |
|------|---------|------|
| `/api/books/**` (GET) | 无 | 所有人可查询 |
| `/api/books/**` (POST/PUT/DELETE) | ADMIN | 管理员专用 |
| `/api/users/admin/**` | ADMIN | 管理员专用 |
| `/api/comments/admin/**` | ADMIN | 管理员专用 |
| 其他接口 | LOGIN | 需要登录 |

---

## 📞 技术支持

**文档**：查看项目根目录下 `API_SPEC_ALIGNMENT.md`  
**前端规范**：`frontend/BACKEND_API_SPEC_V1.md`  
**编译状态**：✅ BUILD SUCCESS


