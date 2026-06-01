# 用户封禁 API 接口文档

本文档描述 TiHu 前端管理员封禁/解封用户功能需要后端提供的接口。默认基础地址为 `http://localhost:9090/api`。

## 1. 通用约定

- 封禁列表、封禁、解封接口仅管理员可调用。
- 时间使用 ISO-8601 本地时间字符串，例如 `2026-06-02T12:00:00`。
- 推荐统一响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {}
}
```

前端兼容 `code` 或 `status`，成功值为数字 `200`。错误信息字段兼容 `message`、`msg`、`errorMessage`。

## 2. 查询封禁列表

`GET /users/bans?page=1&size=1000`

响应示例：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "username": "alice",
        "bannedUntil": "2026-06-02T12:00:00"
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 1000
  }
}
```

前端读取字段：

| 含义 | 推荐字段 | 兼容字段 |
| --- | --- | --- |
| 用户名 | `username` | `user` |
| 解封时间 | `bannedUntil` | `banExpireTime`、`until`、`unbanTime` |

## 3. 封禁用户

`POST /users/ban?username=alice&until=2026-06-02T12:00:00`

参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `username` | 是 | 要封禁的用户名 |
| `until` | 是 | 解封时间，ISO-8601 本地时间 |

前端管理员页面让管理员输入封禁小时数，然后计算出 `until` 传给后端。后端应保存该解封时间，并在该时间之前拒绝用户登录。

成功响应使用通用成功响应即可。

## 4. 解除封禁

`POST /users/unban?username=alice`

参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `username` | 是 | 要解封的用户名 |

成功响应使用通用成功响应即可。前端支持从封禁列表选中用户后直接解除封禁。

## 5. 被封禁用户登录

登录接口仍为：

`POST /users/login`

当用户仍在封禁期内，后端应返回非 200 状态，并带上解封时间：

```json
{
  "code": 403,
  "message": "该用户已被封禁",
  "data": {
    "bannedUntil": "2026-06-02T12:00:00"
  }
}
```

前端会显示：

```text
您已被封禁，解封时间是 2026-06-02T12:00:00
```

如果后端只返回封禁错误但不返回解封时间，前端只能显示“您已被封禁，请联系管理员确认解封时间”。因此后端必须返回 `bannedUntil`、`banExpireTime`、`until` 或 `unbanTime` 中任一字段。

## 6. 前端实际调用清单

| 功能 | 方法 | 路径 |
| --- | --- | --- |
| 查询封禁列表 | GET | `/users/bans?page=1&size=1000` |
| 封禁用户 | POST | `/users/ban?username=...&until=...` |
| 解除封禁 | POST | `/users/unban?username=...` |
| 登录时封禁提示 | POST | `/users/login` |
