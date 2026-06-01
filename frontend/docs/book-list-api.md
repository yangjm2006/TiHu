# 书单 API 接口文档

本文档描述 TiHu 前端书单功能需要后端提供的接口。默认基础地址为 `http://localhost:9090/api`，可通过环境变量 `TIHU_BACKEND_BASE_URL` 或系统属性 `tihu.backend.base-url` 覆盖。

## 1. 通用约定

- 所有书单写操作需要登录。
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

## 2. 书单数据结构

推荐书单对象：

```json
{
  "id": 801,
  "owner": "alice",
  "title": "科幻精选",
  "description": "公开书单",
  "bookIds": [101, 108],
  "publicVisible": true,
  "visibility": "PUBLIC"
}
```

字段兼容：

| 含义 | 推荐字段 | 兼容字段 |
| --- | --- | --- |
| 书单 ID | `id` | `listId` |
| 所有者用户名 | `owner` | `username`、`user` |
| 标题 | `title` | - |
| 简介 | `description` | `intro` |
| 图书 ID 列表 | `bookIds` | `bookIdList`、`books` |
| 是否公开 | `publicVisible` | `isPublic`、`public`、`visible` |
| 可见性枚举 | `visibility` | `privacy`、`mode` |

可见性规则：

- `publicVisible=true` 或 `visibility=PUBLIC`：公开书单，所有用户可在用户主页看到并打开。
- `publicVisible=false` 或 `visibility=PRIVATE`：私密书单，仅书单所有者可见。
- 如果响应缺少可见性字段，前端按公开处理，兼容旧数据。

## 3. 查询我的书单

`GET /book-lists?page=1&size=1000`

前端会拉取列表后按 `owner/username/user` 筛选当前登录用户。

响应示例：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "id": 801,
        "owner": "alice",
        "title": "科幻精选",
        "description": "公开书单",
        "bookIds": [101, 108],
        "publicVisible": true,
        "visibility": "PUBLIC"
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 1000
  }
}
```

列表字段兼容 `records`、`list`、`items`、`data`。如果 `data` 本身是数组，也可以直接返回数组。

## 4. 创建书单

`POST /book-lists?title=科幻精选&description=公开书单&publicVisible=true&visibility=PUBLIC`

参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `title` | 是 | 书单标题 |
| `description` | 否 | 书单简介 |
| `publicVisible` | 是 | `true` 公开，`false` 私密 |
| `visibility` | 否 | `PUBLIC` 或 `PRIVATE`，与 `publicVisible` 表达同一语义 |

前端会同时传 `publicVisible` 和 `visibility`，后端任选一个读取即可。

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "bookList": {
      "id": 801,
      "owner": "alice",
      "title": "科幻精选",
      "description": "公开书单",
      "bookIds": [],
      "publicVisible": true,
      "visibility": "PUBLIC"
    }
  }
}
```

前端读取：`bookList`、`data`、`list`。

## 5. 查询书单详情

`GET /book-lists/{listId}`

权限：

- 书单所有者可以查看自己的公开和私密书单。
- 非所有者只能查看公开书单。
- 非所有者访问私密书单时，建议返回 `403`。
- 非所有者打开公开书单时，前端会以只读模式展示：可以查看书单内图书，也可以打开图书详情，但不能添加、移除图书或修改可见性。

响应字段同书单对象。

## 6. 修改书单可见性

`PUT /book-lists/{listId}/visibility?publicVisible=false&visibility=PRIVATE`

参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `publicVisible` | 是 | `true` 切为公开，`false` 切为私密 |
| `visibility` | 否 | `PUBLIC` 或 `PRIVATE` |

权限：

- 只有书单所有者可修改可见性。

成功响应使用通用成功响应即可。

## 7. 删除书单

`DELETE /book-lists/{listId}`

权限：

- 只有书单所有者可删除。

成功响应使用通用成功响应即可。

## 8. 书单图书管理

添加图书：

`POST /book-lists/{listId}/books?bookId=101`

或：

`POST /book-lists/{listId}/books?bookTitle=三体`

移除图书：

`DELETE /book-lists/{listId}/books?bookId=101`

权限：

- 只有书单所有者可添加或移除图书。
- 同一本书不应在同一书单中重复出现。
- 在书单详情页，用户输入的是图书名称，前端会优先传 `bookTitle`。
- 在图书详情页，前端已知当前图书 ID，会传 `bookId`。
- 如果后端暂不支持 `bookTitle`，前端会尝试通过图书列表解析为 `bookId` 后再调用旧接口；后端长期建议直接支持 `bookTitle`。

## 9. 用户主页中的书单

用户主页接口：

`GET /users/profile/{username}`

返回的 `bookLists` 必须按当前登录用户过滤：

- 当前登录用户查看自己的主页：返回自己的公开和私密书单。
- 当前登录用户查看他人主页：只返回对方公开书单。
- 前端支持在用户主页双击书单进入书单详情。
- 进入自己的书单后可编辑；进入他人的公开书单后只能查看。

示例：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "userInfo": {
      "id": 2,
      "username": "bob"
    },
    "bookLists": [
      {
        "id": 801,
        "owner": "bob",
        "title": "公开书单",
        "description": "所有人可见",
        "bookIds": [101],
        "publicVisible": true,
        "visibility": "PUBLIC"
      }
    ]
  }
}
```

## 10. 前端实际调用清单

| 功能 | 方法 | 路径 |
| --- | --- | --- |
| 查询我的书单 | GET | `/book-lists?page=1&size=1000` |
| 创建书单 | POST | `/book-lists?title=...&description=...&publicVisible=...&visibility=...` |
| 查询书单详情 | GET | `/book-lists/{listId}` |
| 修改可见性 | PUT | `/book-lists/{listId}/visibility?publicVisible=...&visibility=...` |
| 删除书单 | DELETE | `/book-lists/{listId}` |
| 添加图书 | POST | `/book-lists/{listId}/books?bookId=...` |
| 按书名添加图书 | POST | `/book-lists/{listId}/books?bookTitle=...` |
| 移除图书 | DELETE | `/book-lists/{listId}/books?bookId=...` |
| 用户主页书单 | GET | `/users/profile/{username}` |

## 11. 前端交互说明

- 我的书单列表：双击书单可打开详情。
- 用户主页书单列表：双击公开书单可打开详情。
- 书单详情图书列表：双击图书可打开图书详情。
- 从书单详情进入图书详情后，点击返回会回到原书单。
- 从他人主页进入公开书单后，点击返回会回到该用户主页。
