# TiHu V1 后端接口规范（给后端的实现依据）

> 适用范围：当前 JavaFX 前端与后端 Spring Boot 接口对接。
>
> 这份文档以**当前前端代码实际调用的接口**为准，优先保证可对接、可运行、可扩展。
>
> 前端 HTTP 基址默认值来自 `ApiClient`：`http://localhost:9090/api`
>
> 因此本文所有接口路径均以 `/api` 为前缀描述；如果后端采用别的网关前缀，请同步修改前端配置。

---

## 1. 总体约定

### 1.1 统一前缀
- 当前前端默认请求基址：`http://localhost:9090/api`
- 所有接口路径均挂在 `/api` 下
- 示例：
  - `POST /api/users/login`
  - `GET /api/books/1`

### 1.2 请求方式
- `GET`：获取数据
- `POST`：新增/动作类操作
- `PUT`：更新
- `DELETE`：删除/取消

### 1.3 数据格式
- 请求体：`application/json`
- 响应体：`application/json`
- 时间字段：统一使用 **ISO-8601** 字符串，形如：
  - `2026-05-20T13:45:00`
- 建议后端使用 `LocalDateTime` 的标准序列化格式
- 不要返回时间戳数字

### 1.4 登录与鉴权
- V1 所有操作都要求登录后才能执行
- 登录态以 **Session + Cookie** 为准
- 后端登录成功后必须返回：
  - `Set-Cookie`：供前端保存并在后续请求中携带
- 前端 `ApiClient` 也会读取 `Authorization` 响应头并保存，因此后端可以同时兼容返回：
  - `Authorization: <token>`（可选兼容）
  - 但**核心还是 Session Cookie**
- 后续请求必须能识别 `Cookie` 中携带的 session

### 1.5 角色
- `USER`：普通用户
- `ADMIN`：管理员

### 1.6 统一状态码规则
> 说明：当前前端适配层 `RemoteBackendService` 默认把 `code == 200` 当作成功。
>
> 因此本文统一要求：**成功返回 `code = 200`**。

- 成功：`200`
- 参数错误：`400`
- 未登录 / Session 失效：`401`
- 无权限：`403`
- 资源不存在：`404`
- 冲突：`409`
- 服务器错误：`500`

### 1.7 统一响应格式
所有接口都应返回统一 JSON 包装：

```json
{
  "code": 200,
  "message": "OK",
  "data": {}
}
```

失败示例：

```json
{
  "code": 400,
  "message": "用户名长度必须为 2~10",
  "data": null
}
```

### 1.8 统一返回字段说明
- `code`：业务状态码，数字
- `message`：提示信息，字符串
- `data`：业务数据，任意 JSON；失败时可为 `null`

### 1.9 分页约定
分页接口建议统一采用：
- 请求参数：`page`、`size`
- 返回结构建议：

```json
{
  "records": [],
  "total": 0,
  "pages": 0,
  "current": 1,
  "size": 10
}
```

> 前端当前会兼容 `records / items / list` 等字段，但建议后端统一使用 `records`。

---

## 2. 通用数据模型建议

### 2.1 用户对象 `UserDTO`
```json
{
  "id": 1,
  "username": "alice",
  "role": "USER",
  "avatarUrl": "",
  "banEndTime": null,
  "createdAt": "2026-05-20T13:45:00"
}
```

字段说明：
- `id`：用户 ID
- `username`：用户名，唯一，大小写敏感
- `role`：`USER` 或 `ADMIN`
- `avatarUrl`：V1 固定默认头像，可返回空串或默认 URL
- `banEndTime`：封禁截止时间，未封禁为 `null`
- `createdAt`：创建时间

### 2.2 图书对象 `BookDTO`
```json
{
  "id": 1,
  "title": "三体",
  "author": "刘慈欣",
  "description": "...",
  "cover": "",
  "tags": ["科幻", "硬科幻"],
  "averageScore": 8.7,
  "ratingCount": 12,
  "deleted": false
}
```

字段说明：
- `title`：书名，唯一
- `author`：作者，可空
- `description`：简介，可空
- `cover`：V1 使用统一默认封面，可返回空串或默认 URL
- `tags`：标签数组
- `averageScore`：平均分
- `ratingCount`：评分人数
- `deleted`：逻辑删除标记（不展示时可不返回）

### 2.3 评分统计对象 `RatingStatsDTO`
```json
{
  "bookId": 1,
  "averageScore": 8.7,
  "ratingCount": 12,
  "distribution": {
    "1": 0,
    "2": 0,
    "3": 1,
    "4": 0,
    "5": 2,
    "6": 1,
    "7": 2,
    "8": 3,
    "9": 2,
    "10": 1
  },
  "myScore": 8
}
```

### 2.4 评论对象 `CommentDTO`
```json
{
  "id": 1001,
  "bookId": 1,
  "userId": 2,
  "username": "alice",
  "content": "这本书很棒",
  "createTime": "2026-05-20T13:45:00",
  "parentCommentId": null,
  "upVotes": 3,
  "downVotes": 1,
  "deleted": false,
  "recalled": false
}
```

说明：
- 一级评论：`parentCommentId = null`
- 二级回复：`parentCommentId = 一级评论 ID`
- V1 不支持三级及以上回复
- 逻辑删除/撤回后可保留数据，但前端不展示

### 2.5 书单对象 `BookListDTO`
```json
{
  "id": 3001,
  "ownerId": 2,
  "ownerUsername": "alice",
  "title": "我的科幻书单",
  "description": "收集我喜欢的科幻作品",
  "bookIds": [1, 3, 5],
  "createdAt": "2026-05-20T13:45:00"
}
```

### 2.6 关注对象 `FollowDTO`
```json
{
  "followerId": 2,
  "followerUsername": "alice",
  "followeeId": 3,
  "followeeUsername": "bob",
  "createTime": "2026-05-20T13:45:00"
}
```

### 2.7 会话预览 `ConversationPreviewDTO`
```json
{
  "peerId": 3,
  "peerUsername": "bob",
  "lastMessage": "你好",
  "lastTime": "2026-05-20T13:45:00"
}
```

### 2.8 私信消息 `MessageDTO`
```json
{
  "id": 9001,
  "senderId": 2,
  "senderUsername": "alice",
  "receiverId": 3,
  "receiverUsername": "bob",
  "content": "你好",
  "createTime": "2026-05-20T13:45:00"
}
```

### 2.9 封禁信息 `BanInfoDTO`
```json
{
  "userId": 2,
  "username": "alice",
  "banEndTime": "2026-05-23T00:00:00"
}
```

---

## 3. 账号与认证接口

### 3.1 普通用户注册
**POST** `/api/users/register`

请求体：
```json
{
  "username": "alice",
  "password": "Alice123"
}
```

规则：
- 用户名长度 2~10
- 用户名唯一，大小写敏感
- 密码长度 6~12
- 密码必须同时包含数字和英文字符

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 2,
    "username": "alice",
    "role": "USER"
  }
}
```

---

### 3.2 管理员注册
**POST** `/api/users/register?inviteCode=123456`

请求体：
```json
{
  "username": "admin2",
  "password": "Admin123"
}
```

规则：
- `inviteCode` 固定为 `123456`
- 邀请码错误返回 `403` 或 `400`

响应同注册普通用户，`role` 为 `ADMIN`

---

### 3.3 登录
**POST** `/api/users/login`

请求体：
```json
{
  "username": "alice",
  "password": "Alice123"
}
```

成功响应建议同时满足：
1. `Set-Cookie` 返回 Session
2. `Authorization` 可选返回兼容 token
3. `data` 中返回用户信息

响应示例：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "token": "optional-token",
    "userInfo": {
      "id": 2,
      "username": "alice",
      "role": "USER"
    }
  }
}
```

失败示例：
```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

补充规则：
- 如果用户被封禁且未到期，登录失败
- 未登录后续所有接口返回 `401`

---

### 3.4 登出
**POST** `/api/users/logout`

说明：
- 使当前 Session 失效
- 前端调用后应清除登录态

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 3.5 当前登录用户信息
**GET** `/api/users/me`

用于获取当前 Session 对应的用户信息。

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 2,
    "username": "alice",
    "role": "USER"
  }
}
```

---

### 3.6 按用户 ID 获取用户信息
**GET** `/api/users/{id}`

用途：
- 供前端根据用户 ID 反查用户名
- 供私信、关注、列表展示等场景使用

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 3,
    "username": "bob",
    "role": "USER"
  }
}
```

---

### 3.7 根据用户名获取用户主页基础信息
**GET** `/api/users/profile/{username}`

用途：
- 用户主页页
- 前端会通过这个接口拿到：
  - 用户基础信息
  - TA 的评论列表
  - TA 的公开书单列表
  - 关注/粉丝统计
  - 当前登录用户是否已关注 TA

响应示例：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "userInfo": {
      "id": 3,
      "username": "bob",
      "role": "USER"
    },
    "comments": [],
    "bookLists": [],
    "followingCount": 4,
    "followerCount": 10,
    "followedByCurrentUser": true
  }
}
```

---

### 3.8 修改用户名
**PUT** `/api/users/{id}/username?newUsername=NewName`

规则：
- 新用户名 2~10 位
- 唯一，大小写敏感

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 3.9 修改密码
**PUT** `/api/users/{id}/password?oldPassword=Old123&newPassword=New123`

规则：
- 新密码 6~12 位
- 必须同时包含数字和英文字符
- 旧密码不正确返回失败

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

## 4. 图书接口

### 4.1 图书分页列表
**GET** `/api/books?page=1&size=10`

说明：
- 默认分页 10 条/页
- 前端会做书名关键词和标签过滤；后端也可以支持服务端过滤参数
- 建议返回按平均评分倒序

响应示例：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "id": 1,
        "title": "三体",
        "author": "刘慈欣",
        "description": "...",
        "tags": ["科幻"],
        "averageScore": 8.7,
        "ratingCount": 12
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 10
  }
}
```

可选支持的过滤参数（建议实现）：
- `titleKeyword`：书名模糊匹配
- `tags`：标签数组或逗号分隔，要求**同时包含**多个标签（AND）

---

### 4.2 图书详情
**GET** `/api/books/{id}`

前端会依赖该接口同时获取：
- 图书基本信息
- 评分统计
- 评论列表
- 回复列表

建议响应结构：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "bookInfo": {
      "id": 1,
      "title": "三体",
      "author": "刘慈欣",
      "description": "...",
      "cover": "",
      "tags": ["科幻", "硬科幻"]
    },
    "ratings": {
      "bookId": 1,
      "averageScore": 8.7,
      "ratingCount": 12,
      "distribution": {
        "1": 0,
        "2": 0,
        "3": 1,
        "4": 0,
        "5": 2,
        "6": 1,
        "7": 2,
        "8": 3,
        "9": 2,
        "10": 1
      },
      "myScore": 8
    },
    "comments": [],
    "replies": []
  }
}
```

说明：
- `comments`：一级评论
- `replies`：二级回复
- 若后端更喜欢合并返回，也可以直接在 `comments` 中带 `parentCommentId`
- 但前端当前适配层已经兼容 `comments/replies` 两种思路

---

### 4.3 新增图书（管理员）
**POST** `/api/books`

请求体：
```json
{
  "title": "三体",
  "author": "刘慈欣",
  "description": "...",
  "cover": ""
}
```

规则：
- 书名唯一
- 仅管理员可操作
- V1 封面统一默认封面，`cover` 可忽略或固定为空串

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 1,
    "title": "三体",
    "author": "刘慈欣"
  }
}
```

---

### 4.4 编辑图书（管理员）
**PUT** `/api/books/{id}`

请求体：
```json
{
  "title": "三体：修订版",
  "author": "刘慈欣",
  "description": "...",
  "cover": ""
}
```

规则：
- 可修改全部信息
- 书名仍需唯一

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 4.5 删除图书（管理员）
**DELETE** `/api/books/{id}`

规则：
- 逻辑删除
- 前端不再展示该图书
- 该图书相关评分、评论、收藏、书单引用等均应不可见

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

## 5. 评分接口

### 5.1 给图书评分 / 更新评分
**POST** `/api/ratings?bookId=1&score=8`

规则：
- 评分范围 1~10
- 同一用户对同一本书只能有一条评分记录
- 再次评分时视为更新

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 5.2 获取我对某本书的评分
**GET** `/api/ratings/my?bookId=1`

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "bookId": 1,
    "score": 8
  }
}
```

---

### 5.3 获取图书评分统计
**GET** `/api/ratings/book/{bookId}/stats`

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "bookId": 1,
    "averageScore": 8.7,
    "ratingCount": 12,
    "distribution": {
      "1": 0,
      "2": 0,
      "3": 1,
      "4": 0,
      "5": 2,
      "6": 1,
      "7": 2,
      "8": 3,
      "9": 2,
      "10": 1
    }
  }
}
```

---

## 6. 评论与回复接口

### 6.1 发表评论 / 回复
**POST** `/api/comments?bookId=1&content=很好看&parentCommentId=1001`

说明：
- `parentCommentId` 为空或不传：一级评论
- `parentCommentId` 有值：二级回复，只能回复一级评论
- 内容最多 200 字
- 同一用户对同一本书可发多条评论

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "comment": {
      "id": 1002,
      "bookId": 1,
      "userId": 2,
      "username": "alice",
      "content": "很好看",
      "createTime": "2026-05-20T13:45:00",
      "parentCommentId": 1001,
      "upVotes": 0,
      "downVotes": 0
    }
  }
}
```

---

### 6.2 获取某本书的评论
**GET** `/api/comments/book/{bookId}?page=1&size=100`

说明：
- 可返回一级评论和/或全量评论
- 建议按时间倒序
- 前端会把一级评论和回复分开展示或自行过滤

建议响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "id": 1001,
        "bookId": 1,
        "userId": 2,
        "username": "alice",
        "content": "很好看",
        "createTime": "2026-05-20T13:45:00",
        "parentCommentId": null,
        "upVotes": 3,
        "downVotes": 1
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 100
  }
}
```

---

### 6.3 删除自己的评论/回复
**DELETE** `/api/comments/{commentId}`

规则：
- 普通用户只能删除自己的评论/回复
- 删除方式为**逻辑撤回**
- 前端效果：直接不显示

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 6.4 管理员删除任意评论/回复
**DELETE** `/api/comments/admin/{commentId}`

规则：
- 管理员可删除任意评论/回复
- 逻辑删除

响应同上

---

### 6.5 管理员查看全站评论
**GET** `/api/comments/admin/all`

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": 1001,
      "bookId": 1,
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

## 7. 评论点赞 / 点踩接口

### 7.1 点赞
**POST** `/api/comments/{commentId}/like`

说明：
- 同一用户对同一条评论/回复：三态互斥
- 赞 / 踩 / 无
- 可与点踩切换

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 7.2 点踩
**POST** `/api/comments/{commentId}/dislike`

响应同上

---

### 7.3 取消点赞
**DELETE** `/api/comments/{commentId}/like`

说明：
- 取消当前用户对该评论的赞/踩状态（按你后端实现决定具体语义）
- 前端当前主要通过这个接口处理“取消”

响应同上

---

## 8. 收藏接口

### 8.1 添加收藏
**POST** `/api/collections?bookId=1`

规则：
- 收藏对象是图书
- 同一用户对同一本书只能收藏一次

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 8.2 取消收藏
**DELETE** `/api/collections?bookId=1`

响应同上

---

### 8.3 判断是否已收藏
**GET** `/api/collections/check?bookId=1`

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": true
}
```

---

### 8.4 我的收藏列表
**GET** `/api/collections?page=1&size=100`

建议响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "bookId": 1,
        "title": "三体",
        "author": "刘慈欣",
        "tags": ["科幻"],
        "averageScore": 8.7
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 100
  }
}
```

---

## 9. 书单接口

### 9.1 创建书单
**POST** `/api/book-lists?title=我的书单&description=简介`

规则：
- 书单全部公开
- 同一用户可创建多个书单

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 3001,
    "ownerId": 2,
    "ownerUsername": "alice",
    "title": "我的书单",
    "description": "简介",
    "bookIds": []
  }
}
```

---

### 9.2 我的书单列表
**GET** `/api/book-lists?page=1&size=100`

说明：
- 返回当前登录用户的书单
- 也可以返回全站公开书单，若这样做前端要按 owner 过滤
- 推荐只返回当前用户的书单，最符合前端页面含义

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "id": 3001,
        "ownerId": 2,
        "ownerUsername": "alice",
        "title": "我的科幻书单",
        "description": "收集我喜欢的科幻作品",
        "bookIds": [1, 3, 5]
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 100
  }
}
```

---

### 9.3 书单详情
**GET** `/api/book-lists/{listId}`

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 3001,
    "ownerId": 2,
    "ownerUsername": "alice",
    "title": "我的科幻书单",
    "description": "收集我喜欢的科幻作品",
    "bookIds": [1, 3, 5],
    "books": [
      {
        "id": 1,
        "title": "三体",
        "author": "刘慈欣",
        "tags": ["科幻"]
      }
    ]
  }
}
```

---

### 9.4 删除书单
**DELETE** `/api/book-lists/{listId}`

规则：
- 物理删除
- 同时删除关联关系

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 9.5 往书单添加图书
**POST** `/api/book-lists/{listId}/books?bookId=1`

规则：
- 同一本书在同一书单里只能出现一次
- 重复添加应返回 `409`

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 9.6 从书单移除图书
**DELETE** `/api/book-lists/{listId}/books?bookId=1`

响应同上

---

## 10. 关注 / 粉丝接口

### 10.1 关注用户
**POST** `/api/follows?followeeId=3`

规则：
- 单向关注
- 不能重复关注

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 10.2 取关用户
**DELETE** `/api/follows?followeeId=3`

响应同上

---

### 10.3 我关注的人列表
**GET** `/api/follows/followees?page=1&size=100`

响应建议：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "followeeId": 3,
        "followeeUsername": "bob",
        "createTime": "2026-05-20T13:45:00"
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 100
  }
}
```

---

### 10.4 我的粉丝列表
**GET** `/api/follows/followers?page=1&size=100`

响应建议：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "followerId": 4,
        "followerUsername": "charlie",
        "createTime": "2026-05-20T13:45:00"
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 100
  }
}
```

---

## 11. 私信 / 会话接口

### 11.1 发送私信
**POST** `/api/messages?receiverId=3&content=你好`

规则：
- 可给陌生人发私信
- 不要求互相关注
- 纯文本
- 不提供删除
- 不提供未读/已读

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 11.2 会话列表
**GET** `/api/messages/conversations`

规则：
- 返回当前用户的所有会话
- 按最后消息时间倒序

响应建议：
```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "peerId": 3,
      "peerUsername": "bob",
      "lastMessage": "你好",
      "lastTime": "2026-05-20T13:45:00"
    }
  ]
}
```

---

### 11.3 某会话的全部历史消息
**GET** `/api/messages/conversation/{peerId}?page=1&size=100`

规则：
- 进入会话时一次性拉取历史消息
- 无需未读回执

响应建议：
```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": 9001,
      "senderId": 2,
      "senderUsername": "alice",
      "receiverId": 3,
      "receiverUsername": "bob",
      "content": "你好",
      "createTime": "2026-05-20T13:45:00"
    }
  ]
}
```

---

## 12. 管理员接口

### 12.1 获取封禁列表
**GET** `/api/users/admin/bans`

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "userId": 2,
      "username": "alice",
      "banEndTime": "2026-05-23T00:00:00"
    }
  ]
}
```

---

### 12.2 封禁用户
**POST** `/api/users/admin/{userId}/ban?durationSeconds=86400`

说明：
- 由管理员设置封禁时长，单位秒
- 建议后端根据当前时间 + `durationSeconds` 计算封禁截止时间

响应：
```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### 12.3 解封用户
**DELETE** `/api/users/admin/{userId}/ban`

响应同上

---

### 12.4 管理员查看全站评论
见第 6.5 节：`GET /api/comments/admin/all`

---

### 12.5 管理员删除评论/回复
见第 6.4 节：`DELETE /api/comments/admin/{commentId}`

---

## 13. 权限矩阵

### 13.1 普通用户（USER）
可执行：
- 登录/登出
- 修改自己的用户名/密码
- 浏览图书列表、图书详情
- 评分、评论、回复
- 点赞/点踩评论
- 收藏/取消收藏
- 创建/删除自己的书单
- 对书单加书/移书
- 关注/取关
- 查看关注列表、粉丝列表
- 查看任意用户主页
- 发送/接收私信

不可执行：
- 新增/编辑/删除图书
- 封禁/解封用户
- 删除他人评论（除自己逻辑撤回外）

### 13.2 管理员（ADMIN）
除普通用户能力外，还可以：
- 新增/编辑/删除图书
- 查看封禁列表
- 封禁/解封用户
- 删除任意评论/回复

---

## 14. 校验规则汇总

### 14.1 用户名
- 长度：2~10
- 唯一
- 大小写敏感

### 14.2 密码
- 长度：6~12
- 必须同时包含：数字 + 英文字母

### 14.3 图书
- 书名必填且唯一
- 作者、简介、标签可选
- 封面 V1 统一默认封面

### 14.4 评分
- 1~10
- 同书同用户只保留一条记录，可修改

### 14.5 评论/回复
- 纯文本
- 最多 200 字
- 只支持两级回复

### 14.6 书单
- 公开
- 同一本书同一书单内只能出现一次
- 删除书单是物理删除

### 14.7 关注
- 单向关注
- 不允许重复关注

### 14.8 私信
- 一对一
- 允许陌生人发消息
- 不做删除
- 不做未读/已读

---

## 15. 建议后端实现顺序

为了最快跑通前端，建议后端按以下顺序实现：

1. `POST /users/login`
2. `POST /users/register`
3. `GET /users/me`
4. `GET /books`
5. `GET /books/{id}`
6. `POST /ratings?bookId=&score=`
7. `POST /comments?bookId=&content=&parentCommentId=`
8. `GET /comments/book/{bookId}`
9. `POST /collections?bookId=` / `DELETE /collections?bookId=`
10. `GET /book-lists` / `POST /book-lists` / `GET /book-lists/{id}`
11. `POST /follows?followeeId=` / `GET /follows/followees` / `GET /follows/followers`
12. `POST /messages?receiverId=&content=` / `GET /messages/conversations` / `GET /messages/conversation/{peerId}`
13. 管理员接口：图书管理、封禁管理、评论管理

---

## 16. 兼容性说明

### 16.1 关于前端当前适配器
前端当前的 `RemoteBackendService` 会优先读取这些字段名：
- 登录：`data.userInfo`、`data.user`、`data.data`
- 图书详情：`bookInfo`、`book`、`data`
- 评分统计：`ratings`、`ratingSummary`、`data`
- 评论列表：`comments`
- 回复列表：`replies`
- 分页列表：`records`、`items`、`list`

因此后端如果字段命名不同，前端仍可能兼容一部分，但**强烈建议按本文件统一字段**，避免后续维护成本过高。

### 16.2 关于 Cookie
前端会自动保存 `Set-Cookie` 的第一个值并在后续请求中带回，因此后端应确保：
- 登录成功返回 `Set-Cookie`
- 后续接口能正确识别该 Session

### 16.3 关于 Authorization
前端也会记录响应头中的 `Authorization`，但这属于兼容项，不建议替代 Session Cookie。

---

## 17. 最终约束清单

后端实现时必须保证：

- [ ] 所有接口都挂在 `/api` 下
- [ ] 所有成功响应 `code = 200`
- [ ] 所有失败响应带明确 `message`
- [ ] 所有时间字段使用 ISO-8601 字符串
- [ ] 登录态使用 Session + Cookie
- [ ] 评分范围 1~10
- [ ] 评论最多 200 字
- [ ] 回复只支持两级
- [ ] 书单全部公开
- [ ] 私信允许陌生人发送
- [ ] 管理员能删图书、封禁用户、删评论
- [ ] 普通用户不能越权执行管理员操作

---

## 18. 备注

这份文档的目标不是“理论上最漂亮的 REST 设计”，而是**和当前前端直接对接、尽快跑通**。

如果后端后续想做更规范的 V2，可以再统一重构成：
- `/api/v1/...`
- 更标准的分页响应
- 更统一的 DTO 命名
- 更完整的错误码体系

但 V1 请先按本文件实现。

