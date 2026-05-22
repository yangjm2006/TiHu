# ⚠️ 前端对接重要提醒清单

**更新日期**：2026-05-22  
**优先级**：🔴 **重要** - 请前端务必完整阅读此文档

---

## 🚨 关键变更（必读）

### 1. 响应格式变更
```
❌ 旧：{ code: xxx, msg: "...", data: {} }
✅ 新：{ code: xxx, message: "...", data: {} }
```

**前端必须更新**：所有响应处理代码中的 `msg` 改为 `message`

---

### 2. 分页格式统一
以下接口的分页格式已改为标准格式：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [],        // ← 记录列表（必须处理此字段）
    "total": 100,         // ← 总记录数
    "pages": 10,          // ← 总页数
    "current": 1,         // ← 当前页号
    "size": 10            // ← 每页大小
  }
}
```

**受影响接口**（共11个）：
1. `GET /api/books?page=1&size=10`
2. `GET /api/books/search?keyword=...`
3. `GET /api/books/search-by-tags?tags=...`
4. `GET /api/collections?page=1&size=10`
5. `GET /api/book-lists?page=1&size=10`
6. `GET /api/follows/followees?page=1`
7. `GET /api/follows/followers?page=1`
8. `GET /api/follows/user/{id}/followees`
9. `GET /api/follows/user/{id}/followers`
10. `GET /api/comments/book/{id}?page=1`
11. `GET /api/messages/conversation/{id}?page=1`

---

### 3. 新增接口（必须适配）

#### 接口3.1：按ID获取用户
```
GET /api/users/{id}
```
**用途**：当需要通过用户ID查询用户信息时使用（如获取发件人信息）

**响应**：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 2,
    "username": "alice",
    "role": "USER",
    "avatarUrl": null,
    "banEndTime": null,
    "createdAt": "2026-05-20T13:45:00"
  }
}
```

---

#### 接口3.2：获取用户主页（改进版）
```
GET /api/users/profile/{username}
```
**改进**：现在返回完整的用户主页数据（包含评论、书单、关注信息）

**新响应结构**：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "userInfo": {
      "id": 3,
      "username": "bob",
      "role": "USER",
      "avatarUrl": null,
      "banEndTime": null,
      "createdAt": "2026-05-20T13:45:00"
    },
    "comments": [
      {
        "id": 101,
        "bookId": 1,
        "userId": 3,
        "username": "bob",
        "content": "很好看",
        "createTime": "2026-05-20T13:45:00",
        "parentCommentId": null,
        "upVotes": 3,
        "downVotes": 1
      }
    ],
    "bookLists": [
      {
        "id": 3001,
        "userId": 3,
        "title": "我的科幻书单",
        "description": "收集我喜欢的科幻作品",
        "cover": null,
        "isPublic": true,
        "createTime": "2026-05-20T13:45:00",
        "updateTime": "2026-05-20T13:45:00"
      }
    ],
    "followingCount": 10,           // ← 关注数
    "followerCount": 5,             // ← 粉丝数
    "followedByCurrentUser": true   // ← 当前用户是否已关注TA
  }
}
```

**前端需要适配**：
- `comments` - 用户的评论列表（不是对他的评论）
- `bookLists` - 用户创建的书单
- `followingCount` / `followerCount` - 自动统计，无需另外调用
- `followedByCurrentUser` - 显示是否关注按钮状态

---

#### 接口3.3：管理后台 - 获取封禁列表
```
GET /api/users/admin/bans
```
**用途**：管理员查看被封禁用户列表

**响应**：
```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": 2,
      "username": "alice",
      "role": "USER",
      "avatarUrl": null,
      "banEndTime": "2026-05-23T00:00:00",
      "createdAt": "2026-05-20T13:45:00"
    }
  ]
}
```

---

#### 接口3.4：管理后台 - 获取全站评论
```
GET /api/comments/admin/all
```
**用途**：管理员查看全站所有评论（用于审核/删除）

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

## 📋 前端工程师检查清单

### 阶段1：代码审查（必做）
- [ ] 全局搜索所有 `response.msg` 并改为 `response.message`
- [ ] 全局搜索所有分页接口，确保正确解析 `data.records`
- [ ] 检查是否有分页偏移问题（MyBatis-Plus 从 page=1 开始）
- [ ] 检查 API 基址是否配置为 `http://localhost:9090/api`

### 阶段2：接口适配（必做）
- [ ] 确认是否需要适配新的 4 个接口
- [ ] 检查用户主页页面是否能正确展示评论、书单列表
- [ ] 检查关注按钮状态是否由 `followedByCurrentUser` 控制
- [ ] 管理后台页面是否需要添加新的审核界面

### 阶段3：Cookie 处理（必做）
- [ ] 确认登录后是否正确保存 `Set-Cookie`
- [ ] 确认所有后续请求都正确携带 Cookie
- [ ] 测试 Session 过期后是否返回 401（需要重新登录）

### 阶段4：测试（必做）
- [ ] 单元测试：每个新改接口至少测试一次
- [ ] 集成测试：打通整个用户流程（注册→登录→浏览→评分→评论→关注）
- [ ] 分页测试：检查所有分页接口是否正常
- [ ] 权限测试：验证管理员接口是否拒绝普通用户
- [ ] 错误处理：验证各种错误码是否正确处理

### 阶段5：发布前（必做）
- [ ] 与后端确认可用的测试服务器地址
- [ ] 验证生产环境配置（数据库地址、Redis地址等）
- [ ] 确认所有 API 端点可访问
- [ ] 进行端到端测试

---

## 🔧 代码示例

### 示例1：处理新分页格式
```javascript
// 旧代码
function processBooks(response) {
    const books = response.data.records || response.data;  // 兼容处理
    return books;
}

// 新代码（推荐）
function processBooks(response) {
    const { records, total, pages, current, size } = response.data;
    return {
        items: records,
        pagination: { total, pages, current, size }
    };
}
```

### 示例2：处理响应错误
```javascript
// 旧代码
if (response.msg) {
    alert(response.msg);
}

// 新代码
if (response.code !== 200) {
    alert(response.message);  // ← 改为 message
}
```

### 示例3：获取用户主页
```javascript
async getProfilecomAPI getProfile(username) {
    const response = await fetch(`/api/users/profile/${username}`);
    const { userInfo, comments, bookLists, followingCount, followerCount, followedByCurrentUser } = response.data;
    
    // 显示用户信息
    showUser(userInfo);
    
    // 显示评论列表
    showComments(comments);
    
    // 显示书单列表
    showBookLists(bookLists);
    
    // 更新关注按钮
    updateFollowButton(followedByCurrentUser);
    
    // 显示统计数据
    showStats({ followingCount, followerCount });
}
```

---

## ⚠️ 常见错误避坑指南

### ❌ 错误1：忘记改 msg 为 message
```javascript
// ❌ 错误
alert(response.msg);  // 会显示 undefined

// ✅ 正确
alert(response.message);
```

### ❌ 错误2：直接用 response.data 作为分页列表
```javascript
// ❌ 错误（会导致列表显示异常）
this.items = response.data;  // response.data 是整个分页对象

// ✅ 正确
this.items = response.data.records;
this.total = response.data.total;
```

### ❌ 错误3：分页页码混乱
```javascript
// ❌ 错误（MyBatis-Plus 从 1 开始）
fetchBooks(page: 0, size: 10);  // 会导致查询第0页不存在

// ✅ 正确
fetchBooks(page: 1, size: 10);  // 查询第1页（包含前10条记录）
```

### ❌ 错误4：不处理 401 不登录异常
```javascript
// ❌ 错误（用户会被卡在当前页面）
if (response.code === 401) {
    // 什么都不做
}

// ✅ 正确
if (response.code === 401) {
    this.goToLoginPage();
    alert('您的登录已过期，请重新登录');
}
```

---

## 📞 常见问题 Q&A

### Q1：为什么我的用户主页显示的关注数为 0？
**A**：检查 `followingCount` 和 `followerCount` 是否被正确绑定到 UI。这两个字段现在由后端自动统计，无需前端另外调用其他接口。

### Q2：分页接口为什么有时显示不全？
**A**：确保你使用的是 `response.data.records` 而非 `response.data`。新格式中，数据项在 `records` 中。

### Q3：为什么有些用户看不到"关注"按钮？
**A**：检查是否正确处理了 `followedByCurrentUser` 这个新字段。这个字段决定了按钮的显示状态。

### Q4：Cookie 问题导致总是 401？
**A**：
1. 确认登录成功后是否有 `Set-Cookie` 响应头
2. 确认后续请求是否在请求头中携带 `Cookie`
3. 检查浏览器开发者工具的网络标签签查看 Cookie 是否正确发送

### Q5：用户主页加载很慢？
**A**：这是因为现在一个请求需要查询多个数据（用户、评论、书单、关注数）。建议：
1. 在后端启用缓存（Redis）
2. 在前端添加加载动画提示用户
3. 考虑分离为多个请求异步加载

---

## 📞 沟通渠道

如有问题或建议，请通过以下方式反馈：
1. **技术问题**：查看 `API_QUICK_REFERENCE.md` 快速参考
2. **详细文档**：查看 `IMPLEMENTATION_GUIDE.md` 完整指南
3. **故障排除**：查看 `HELP.md` 故障排除指南

---

## ✅ 对接完成标志

当以下条件都满足时，表示对接完成：

- [x] 前端已更新所有 `msg` → `message`
- [x] 前端已适配所有分页接口的新格式
- [x] 前端已实现新增的 4 个接口
- [x] 用户主页能正确展示所有信息（评论、书单、关注数）
- [x] 登录/登出流程正常工作
- [x] 管理后台新功能可用
- [x] Cookie/Session 认证正常
- [x] 所有错误情况都有相应提示
- [x] 集成测试完全通过

---

**最后提醒**：  
本文档中的所有变更都是为了与前端规范保持一致，请务必完整阅读并逐项检查。如有疑问，请及时与后端开发团队沟通。

**更新日期**：2026-05-22  
**优先级**：🔴 **重要**

