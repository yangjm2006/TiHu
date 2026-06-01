# 评论与回复 API 接口文档

本文档描述 TiHu 前端评论功能需要后端提供的接口。默认基础地址为 `http://localhost:9090/api`，可通过环境变量 `TIHU_BACKEND_BASE_URL` 或系统属性 `tihu.backend.base-url` 覆盖。

## 1. 通用约定

- 所有接口需要登录，管理员接口需要管理员权限。
- 前端登录后会携带后端返回的 `Authorization` header 或 session cookie。
- 推荐统一响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {}
}
```

前端兼容 `code` 或 `status`，成功值为数字 `200`。数据字段兼容 `data` 或 `result`。错误信息字段兼容 `message`、`msg`、`errorMessage`。

## 2. 评论数据结构

推荐评论对象：

```json
{
  "id": 501,
  "username": "alice",
  "content": "这本书不错",
  "createTime": "2026-05-31T12:00:00",
  "parentId": null,
  "upVotes": 3,
  "downVotes": 0
}
```

字段兼容：

| 含义 | 推荐字段 | 兼容字段 |
| --- | --- | --- |
| 评论 ID | `id` | `commentId` |
| 用户名 | `username` | `user`、`nickname` |
| 内容 | `content` | `text` |
| 创建时间 | `createTime` | `time`、`createdAt` |
| 父评论 ID | `parentId` | `replyTo` |
| 点赞数 | `upVotes` | `upvoteCount`、`likes` |
| 点踩数 | `downVotes` | `downvoteCount`、`dislikes` |

`parentId = null` 表示一级评论；`parentId = 一级评论 ID` 表示回复。当前前端只允许回复一级评论。

## 3. 查询图书评论

`GET /comments/book/{bookId}?page=1&size=100`

响应示例：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "id": 501,
        "username": "alice",
        "content": "这本书不错",
        "createTime": "2026-05-31T12:00:00",
        "parentId": null,
        "upVotes": 3,
        "downVotes": 0
      },
      {
        "id": 502,
        "username": "bob",
        "content": "同意",
        "createTime": "2026-05-31T12:05:00",
        "parentId": 501,
        "upVotes": 1,
        "downVotes": 0
      }
    ],
    "total": 2,
    "pages": 1,
    "current": 1,
    "size": 100
  }
}
```

列表字段兼容 `records`、`list`、`items`、`data`。如果 `data` 本身是数组，也可以直接返回数组。

## 4. 发表评论或回复

`POST /comments?bookId=101&content=内容&parentCommentId=501`

参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `bookId` | 是 | 图书 ID |
| `content` | 是 | 评论或回复内容 |
| `parentCommentId` | 否 | 回复时传一级评论 ID；发表评论时不传 |

前端请求体为空 JSON：`{}`。

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "comment": {
      "id": 503,
      "username": "alice",
      "content": "回复内容",
      "createTime": "2026-05-31T12:10:00",
      "parentId": 501,
      "upVotes": 0,
      "downVotes": 0
    }
  }
}
```

前端读取新增评论：`comment` 或 `data`。

## 5. 删除评论

删除自己的评论：

`DELETE /comments/{commentId}`

管理员删除任意评论：

`DELETE /comments/admin/{commentId}`

如果删除的是一级评论，建议后端同时删除它下面的回复，避免前端刷新后出现找不到父评论的孤儿回复。成功响应使用通用成功响应即可。

## 6. 点赞、点踩、取消投票

`POST /comments/{commentId}/votes?target=1`

参数：

| `target` | 说明 |
| --- | --- |
| `1` | 点赞 |
| `-1` | 点踩 |
| `0` | 取消投票 |

前端按钮目前发送 `1` 或 `-1`。如果用户重复点击同一个方向，建议后端切换为取消投票；如果从点赞切换到点踩，需要减少点赞数并增加点踩数，反向同理。

成功响应使用通用成功响应即可。前端会重新查询图书详情或评论列表刷新计数。

## 7. 管理员查询全站评论

`GET /comments/admin?page=1&size=1000`

响应格式同图书评论列表。建议按 `createTime` 倒序返回，便于管理员处理最新评论。

## 8. 前端实际调用清单

| 功能 | 方法 | 路径 |
| --- | --- | --- |
| 查询图书评论 | GET | `/comments/book/{bookId}?page=1&size=100` |
| 发表评论/回复 | POST | `/comments?bookId=...&content=...&parentCommentId=...` |
| 删除自己的评论 | DELETE | `/comments/{commentId}` |
| 管理员删除评论 | DELETE | `/comments/admin/{commentId}` |
| 点赞/点踩 | POST | `/comments/{commentId}/votes?target=...` |
| 管理员查询全站评论 | GET | `/comments/admin?page=1&size=1000` |
