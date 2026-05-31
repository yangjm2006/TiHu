# 私信对话 API 接口文档

本文档描述前端私信对话功能需要后端提供的接口。默认基础地址为 `http://localhost:9090/api`。

## 1. 通用约定

- 所有接口需要登录。
- 前端会携带登录时后端返回的 `Authorization` header 或 session cookie。
- 响应推荐统一格式：

```json
{
  "code": 200,
  "message": "OK",
  "data": {}
}
```

- 前端兼容 `code` 或 `status` 作为状态字段；值为 `200` 表示成功。
- 前端兼容 `data` 或 `result` 作为业务数据字段。
- 错误信息请放在 `message`、`msg` 或 `errorMessage`。
- 时间字段使用 ISO-8601 本地时间：`2026-05-31T12:00:00`。

分页列表推荐：

```json
{
  "records": [],
  "total": 0,
  "pages": 1,
  "current": 1,
  "size": 20
}
```

前端读取列表时兼容 `records`、`list`、`items`、`data`，也兼容 `data` 本身直接是数组。

## 2. 用户名解析接口

私信发送和打开会话时，前端会优先把用户名解析成用户 ID。后端至少提供以下任一接口即可，推荐提供第一个：

1. `GET /users/profile/{username}`
2. `GET /users/by-username/{username}`
3. `GET /users/{username}`

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 2,
    "username": "bob"
  }
}
```

前端读取用户 ID：`id` 或 `userId`。

如果无法解析用户 ID，前端会退回使用用户名参数发送消息和查询会话。

## 3. 会话列表

`GET /messages/conversations`

说明：返回当前登录用户的所有私信会话，建议按最后一条消息时间倒序排列。前端也会本地再排序一次。

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "peer": "bob",
        "lastMessage": "你好，最近在读什么？",
        "lastTime": "2026-05-31T12:00:00"
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 20
  }
}
```

前端读取字段：

| 前端含义 | 推荐字段 | 兼容字段 |
| --- | --- | --- |
| 对方用户名 | `peer` | `username`、`user`、`peerUsername`、`targetUsername`、`otherUsername` |
| 最后一条消息 | `lastMessage` | `lastContent`、`content`、`message` |
| 最后消息时间 | `lastTime` | `time`、`updatedAt`、`createTime`、`createdAt` |

如果对方用户是对象，前端也兼容：

```json
{
  "peerUser": {
    "id": 2,
    "username": "bob"
  },
  "lastContent": "你好",
  "updatedAt": "2026-05-31T12:00:00"
}
```

对象字段兼容：`peerUser`、`targetUser`、`otherUser`、`userInfo`、`user`。

## 4. 查询单个会话消息

优先使用用户 ID：

`GET /messages/conversation/{peerId}?page=1&size=100`

如果前端无法解析用户 ID，会使用用户名：

`GET /messages/conversation/{peerUsername}?page=1&size=100`

说明：返回当前登录用户与指定用户之间的消息。建议按发送时间升序返回；前端也会本地按时间升序排序。

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "id": 1001,
        "from": "alice",
        "to": "bob",
        "content": "你好",
        "time": "2026-05-31T12:00:00"
      },
      {
        "id": 1002,
        "from": "bob",
        "to": "alice",
        "content": "你好，最近在读什么？",
        "time": "2026-05-31T12:01:00"
      }
    ],
    "total": 2,
    "pages": 1,
    "current": 1,
    "size": 100
  }
}
```

前端读取字段：

| 前端含义 | 推荐字段 | 兼容字段 |
| --- | --- | --- |
| 发送方用户名 | `from` | `sender`、`senderUsername`、`fromUsername`、`senderName` |
| 接收方用户名 | `to` | `receiver`、`receiverUsername`、`toUsername`、`receiverName` |
| 内容 | `content` | `message`、`text` |
| 时间 | `time` | `createTime`、`createdAt` |

如果发送方/接收方是对象，前端也兼容：

```json
{
  "senderUser": {
    "id": 1,
    "username": "alice"
  },
  "receiverUser": {
    "id": 2,
    "username": "bob"
  },
  "message": "你好",
  "createdAt": "2026-05-31T12:00:00"
}
```

发送方对象字段兼容：`senderUser`、`senderInfo`、`sender`。

接收方对象字段兼容：`receiverUser`、`receiverInfo`、`receiver`。

对象内部用户名字段兼容：`username`、`user`、`nickname`、`name`。

## 5. 发送私信

优先使用用户 ID：

`POST /messages?receiverId=2&content=你好`

如果前端无法解析用户 ID，会使用用户名：

`POST /messages?receiverUsername=bob&content=你好`

请求体：前端当前发送空 JSON `{}`，后端应从 query 参数读取。

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 1003,
    "from": "alice",
    "to": "bob",
    "content": "你好",
    "time": "2026-05-31T12:05:00"
  }
}
```

前端当前不依赖返回的消息对象，只要成功即可；建议返回完整消息，方便后续优化成发送后局部追加。

业务规则建议：

- `content` 不能为空。
- `receiverId` 或 `receiverUsername` 必须能定位到用户。
- 不允许给自己发送私信。
- 发送第一条消息时，如果会话不存在，后端自动创建会话。

## 6. 前端交互依赖

当前前端私信功能会执行这些动作：

- 打开“私信会话”页面时调用 `GET /messages/conversations`
- 在会话页输入关键字时只做本地筛选，不额外调用接口
- 双击或点击会话时打开聊天页
- 发起/打开会话时，如果首条消息不为空，会先调用 `POST /messages`
- 打开聊天页时调用 `GET /messages/conversation/{peer}`
- 点击“发送”时调用 `POST /messages`
- 点击“刷新”时重新调用当前聊天消息列表接口

## 7. 可选扩展接口

以下接口当前前端没有强依赖，但如果后端计划支持未读状态，建议按这个方向新增，后续前端可以接入：

### 7.1 标记会话已读

`POST /messages/conversation/{peerIdOrUsername}/read`

成功响应：通用成功响应即可。

### 7.2 返回未读数

在 `GET /messages/conversations` 的单条会话中增加：

```json
{
  "peer": "bob",
  "lastMessage": "你好",
  "lastTime": "2026-05-31T12:00:00",
  "unreadCount": 2
}
```

当前前端会忽略 `unreadCount`，不影响兼容。
