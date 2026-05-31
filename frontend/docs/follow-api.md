# 关注与粉丝 API 接口文档

本文档描述 TiHu 前端关注/粉丝列表使用的后端接口。默认基础地址为 `http://localhost:9090/api`，可通过环境变量 `TIHU_BACKEND_BASE_URL` 或系统属性 `tihu.backend.base-url` 覆盖。

## 通用约定

- 登录后请求会携带 `Authorization` 响应头中返回的 token；如果后端使用 Cookie 会话，也会自动携带 `Set-Cookie` 中的会话 Cookie。
- 响应建议使用统一包裹格式：

```json
{
  "code": 200,
  "message": "OK",
  "data": {}
}
```

- 前端判断 `code` 或 `status` 为 `200` 时表示成功；`data` 或 `result` 会被当作业务数据。
- 分页列表建议返回：

```json
{
  "records": [],
  "total": 0,
  "pages": 1,
  "current": 1,
  "size": 20
}
```

前端也兼容 `records`、`list`、`items`、`data` 作为列表字段。

## 关注用户

`POST /follows`

请求参数：

| 参数 | 位置 | 必填 | 说明 |
| --- | --- | --- | --- |
| followeeId | query | 二选一 | 被关注用户 ID。前端能解析用户 ID 时优先传这个字段 |
| followeeUsername | query | 二选一 | 被关注用户名。前端无法解析 ID 时使用 |

示例：

```http
POST /api/follows?followeeId=2
Authorization: <token>
```

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

业务规则：

- 不能关注自己。
- 重复关注应保持幂等，建议仍返回成功。

## 取消关注

`DELETE /follows`

请求参数同“关注用户”。

示例：

```http
DELETE /api/follows?followeeId=2
Authorization: <token>
```

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

业务规则：

- 未关注时取消关注建议保持幂等，返回成功。

## 查询当前登录用户的关注列表

`GET /follows/followees`

请求参数：

| 参数 | 位置 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | query | 否 | 页码，前端默认传 `1` |
| size | query | 否 | 每页数量，前端默认传 `1000` |

示例：

```http
GET /api/follows/followees?page=1&size=1000
Authorization: <token>
```

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "username": "bob"
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 1000
  }
}
```

## 查询当前登录用户的粉丝列表

`GET /follows/followers`

请求参数同“查询当前登录用户的关注列表”。

示例：

```http
GET /api/follows/followers?page=1&size=1000
Authorization: <token>
```

## 查询指定用户的关注列表

`GET /follows/user/{userId}/followees`

请求参数：

| 参数 | 位置 | 必填 | 说明 |
| --- | --- | --- | --- |
| userId | path | 是 | 被查看用户 ID |
| page | query | 否 | 页码，前端默认传 `1` |
| size | query | 否 | 每页数量，前端默认传 `1000` |

示例：

```http
GET /api/follows/user/2/followees?page=1&size=1000
Authorization: <token>
```

## 查询指定用户的粉丝列表

`GET /follows/user/{userId}/followers`

请求参数同“查询指定用户的关注列表”。

示例：

```http
GET /api/follows/user/2/followers?page=1&size=1000
Authorization: <token>
```

## 用户资料接口依赖

关注/粉丝列表查看他人数据、关注/取关他人时，前端需要把用户名解析为用户 ID。解析顺序如下：

1. `GET /users/profile/{username}`
2. `GET /users/by-username/{username}`
3. `GET /users/{username}`

上述任一接口返回 `id` 或 `userId` 即可。`/users/profile/{username}` 同时用于用户主页，应至少返回：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "userInfo": {
      "id": 2,
      "username": "bob"
    },
    "followingCount": 1,
    "followerCount": 3,
    "followedByCurrentUser": true,
    "comments": [],
    "bookLists": []
  }
}
```

## 列表记录兼容字段

关注/粉丝列表中的单条记录，前端会按以下字段解析用户名：

- 顶层字段：`username`、`user`、`followeeUsername`、`followerUsername`、`nickname`、`name`
- 嵌套字段：`userInfo`、`userInfoVO`、`user`、`followee`、`follower`、`profile` 内的 `username`/`nickname`/`name`

推荐关注列表记录：

```json
{
  "followee": {
    "id": 2,
    "username": "bob"
  }
}
```

推荐粉丝列表记录：

```json
{
  "follower": {
    "id": 1,
    "username": "alice"
  }
}
```

## 错误响应

建议错误响应仍使用统一包裹：

```json
{
  "code": 400,
  "message": "不能关注自己",
  "data": null
}
```

前端会展示 `message`、`msg` 或 `errorMessage` 字段中的错误信息。
