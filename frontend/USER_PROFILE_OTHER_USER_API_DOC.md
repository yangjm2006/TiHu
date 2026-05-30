# 用户主页查看其他用户 API 接口文档

## 1. 功能说明

该功能用于在前端“用户主页”页面查看任意用户的公开主页信息，并支持继续查看该用户的关注列表、粉丝列表，以及打开其公开书单。

当前后端已提供并可直接对接以下能力：

- 获取指定用户主页信息
- 获取当前登录用户的关注列表 / 粉丝列表
- 获取指定用户的关注列表 / 粉丝列表
- 关注 / 取关指定用户
- 检查当前登录用户是否已关注指定用户

---

## 2. 接口总览

| 功能 | 方法 | 路径 |
|------|------|------|
| 查看用户主页 | GET | `/api/users/profile/{username}` |
| 查看当前登录用户关注列表 | GET | `/api/follows/followees?page=1&size=10` |
| 查看当前登录用户粉丝列表 | GET | `/api/follows/followers?page=1&size=10` |
| 查看指定用户关注列表 | GET | `/api/follows/user/{userId}/followees?page=1&size=10` |
| 查看指定用户粉丝列表 | GET | `/api/follows/user/{userId}/followers?page=1&size=10` |
| 关注用户 | POST | `/api/follows?followeeId={id}` |
| 取消关注 | DELETE | `/api/follows?followeeId={id}` |
| 检查是否已关注 | GET | `/api/follows/check?followeeId={id}` |

> 说明：当前后端关注/取关接口仅支持 `followeeId`，不支持 `followeeUsername`。

---

## 3. 查看用户主页

### 3.1 请求

**Method**: `GET`

**URL**:

```http
/api/users/profile/bob
```

### 3.2 请求参数

| 参数名 | 类型 | 必填 | 位置 | 说明 |
|--------|------|------|------|------|
| username | string | 是 | path | 要查看的目标用户名 |

### 3.3 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userInfo": {
      "id": 2,
      "username": "bob",
      "avatar": "https://example.com/avatar.png",
      "bio": "读书爱好者"
    },
    "comments": [
      {
        "id": 501,
        "userId": 2,
        "content": "这本书很不错",
        "createTime": "2026-05-25T13:45:00"
      }
    ],
    "bookLists": [
      {
        "id": 801,
        "userId": 2,
        "title": "科幻精选",
        "description": "公开书单"
      }
    ],
    "followingCount": 12,
    "followerCount": 8,
    "followedByCurrentUser": true
  }
}
```

### 3.4 字段说明

#### 顶层 `data`

| 字段 | 类型 | 说明 |
|------|------|------|
| userInfo | object | 用户基础信息对象，直接返回后端用户实体 |
| comments | array | 该用户评论列表，按 `createTime` 倒序返回 |
| bookLists | array | 该用户书单列表 |
| followingCount | number | 关注数 |
| followerCount | number | 粉丝数 |
| followedByCurrentUser | boolean | 当前登录用户是否已关注该用户；未登录时固定为 `false` |

#### `userInfo`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 用户 id |
| username | string | 用户名 |
| avatar | string/null | 头像 |
| bio | string/null | 简介 |

> 说明：`userInfo` 为后端直接返回的 [`User`](../backend/src/main/java/com/tihu/backend/entity/User.java) 实体，实际字段以后端实体定义为准。

#### `comments[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 评论 id |
| userId | number | 评论所属用户 id |
| content | string | 评论内容 |
| createTime | string | 评论创建时间 |

#### `bookLists[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 书单 id |
| userId | number | 书单所属用户 id |
| title | string | 书单标题 |
| description | string | 书单简介 |

### 3.5 与前端文档差异说明

当前后端实现与部分前端预期存在以下差异，联调时需以前者为准：

- 用户主页路径是 `/api/users/profile/{username}`，不是 `/api/users/profile?username=...`
- 顶层用户信息放在 `data.userInfo` 中，不是直接平铺在 `data` 下
- 评论时间字段为 `createTime`，不是 `createdAt`
- 书单项当前未返回 `owner`、`bookIds` 等前端文档中的扩展字段
- 当前代码未体现“仅公开评论 / 仅公开书单”的可见性过滤逻辑，而是按用户 id 查询评论和书单

---

## 4. 查看当前登录用户关注列表

### 4.1 请求

**Method**: `GET`

**URL**:

```http
/api/follows/followees?page=1&size=10
```

### 4.2 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码，默认 1 |
| size | number | 否 | 每页数量，默认 10 |

### 4.3 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 3,
        "username": "carol"
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 10
  }
}
```

### 4.4 说明

该接口返回当前登录用户自己的关注列表，不接收目标用户名或目标用户 id 参数。

---

## 5. 查看当前登录用户粉丝列表

### 5.1 请求

**Method**: `GET`

**URL**:

```http
/api/follows/followers?page=1&size=10
```

### 5.2 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | number | 否 | 页码，默认 1 |
| size | number | 否 | 每页数量，默认 10 |

### 5.3 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "username": "alice"
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 10
  }
}
```

### 5.4 说明

该接口返回当前登录用户自己的粉丝列表，不接收目标用户名或目标用户 id 参数。

---

## 6. 查看指定用户关注列表

### 6.1 请求

**Method**: `GET`

**URL**:

```http
/api/follows/user/2/followees?page=1&size=10
```

### 6.2 请求参数

| 参数名 | 类型 | 必填 | 位置 | 说明 |
|--------|------|------|------|------|
| userId | number | 是 | path | 目标用户 id |
| page | number | 否 | query | 页码，默认 1 |
| size | number | 否 | query | 每页数量，默认 10 |

### 6.3 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 3,
        "username": "carol"
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 10
  }
}
```

### 6.4 说明

后端已支持查看指定用户的关注列表，但路径参数使用 `userId`，并不是前端文档中建议的 `username` 查询参数形式。

---

## 7. 查看指定用户粉丝列表

### 7.1 请求

**Method**: `GET`

**URL**:

```http
/api/follows/user/2/followers?page=1&size=10
```

### 7.2 请求参数

| 参数名 | 类型 | 必填 | 位置 | 说明 |
|--------|------|------|------|------|
| userId | number | 是 | path | 目标用户 id |
| page | number | 否 | query | 页码，默认 1 |
| size | number | 否 | query | 每页数量，默认 10 |

### 7.3 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "username": "alice"
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 10
  }
}
```

---

## 8. 关注用户

### 8.1 请求

**Method**: `POST`

**URL**:

```http
/api/follows?followeeId=2
```

### 8.2 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| followeeId | number | 是 | 要关注的目标用户 id |

### 8.3 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 8.4 说明

当前仅支持 `followeeId`，如果传入自己 id，后端会返回“不能关注自己”。

---

## 9. 取消关注用户

### 9.1 请求

**Method**: `DELETE`

**URL**:

```http
/api/follows?followeeId=2
```

### 9.2 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| followeeId | number | 是 | 要取消关注的目标用户 id |

### 9.3 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 10. 检查是否已关注

### 10.1 请求

**Method**: `GET`

**URL**:

```http
/api/follows/check?followeeId=2
```

### 10.2 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| followeeId | number | 是 | 目标用户 id |

### 10.3 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

---

## 11. 后端实现对应位置

本接口文档当前已按后端实现对齐，核心代码位置如下：

- [`UserController.getUserProfile()`](../backend/src/main/java/com/tihu/backend/controller/UserController.java:114)
- [`UserServiceImpl.getUserProfile()`](../backend/src/main/java/com/tihu/backend/service/impl/UserServiceImpl.java:226)
- [`FollowController.followUser()`](../backend/src/main/java/com/tihu/backend/controller/FollowController.java:26)
- [`FollowController.unfollowUser()`](../backend/src/main/java/com/tihu/backend/controller/FollowController.java:37)
- [`FollowController.getFollowees()`](../backend/src/main/java/com/tihu/backend/controller/FollowController.java:48)
- [`FollowController.getFollowers()`](../backend/src/main/java/com/tihu/backend/controller/FollowController.java:59)
- [`FollowController.getUserFollowees()`](../backend/src/main/java/com/tihu/backend/controller/FollowController.java:70)
- [`FollowController.getUserFollowers()`](../backend/src/main/java/com/tihu/backend/controller/FollowController.java:80)
- [`FollowController.checkFollowing()`](../backend/src/main/java/com/tihu/backend/controller/FollowController.java:90)

---

## 12. 对齐结论

相较原前端文档，当前后端已经支持“查看其他用户主页”和“按用户 id 查看其他用户的关注/粉丝列表”，但接口路径、返回结构、部分字段命名以及关注接口参数形式存在差异。

本文件已按后端现状完成修正，前后端联调时应以本文档为准。
