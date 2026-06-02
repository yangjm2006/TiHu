# TiHu 前端所需 API 接口文档

本文档按当前前端代码 `RemoteBackendService` 整理，目的是让后端按前端实际调用路径和字段对齐。

默认基础地址：`http://localhost:9090/api`

前端可通过以下方式覆盖：

- 环境变量：`TIHU_BACKEND_BASE_URL`
- JVM 参数：`-Dtihu.backend.base-url=http://host:port/api`

## 1. 通用协议

### 1.1 请求

- 请求体为 JSON 时，前端发送 `Content-Type: application/json; charset=UTF-8`
- 前端所有请求发送 `Accept: application/json`
- 登录后如果响应头返回 `Authorization`，前端后续请求会原样携带该 header
- 如果响应头返回 `Set-Cookie`，前端后续请求会携带该 session cookie

### 1.2 统一响应格式

推荐所有接口返回：

```json
{
  "code": 200,
  "message": "OK",
  "data": {}
}
```

前端兼容：

- 状态字段：`code` 或 `status`
- 成功值：数字 `200`
- 数据字段：`data` 或 `result`
- 错误信息字段：`message`、`msg`、`errorMessage`

如果 `code/status` 存在且不是 `200`，前端认为远程接口失败，会展示错误信息。默认开发模式下，部分网络异常会回退本地 mock；联调/生产可开启严格远程模式禁用回退。

严格远程模式开关：

- 环境变量：`TIHU_REMOTE_STRICT=true`
- JVM 参数：`-Dtihu.remote.strict=true`

开启后，远程接口请求失败会直接抛错，不再回退本地 mock，便于发现后端接口问题。

### 1.3 分页响应

分页接口推荐：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [],
    "total": 0,
    "pages": 1,
    "current": 1,
    "size": 20
  }
}
```

前端读取列表时兼容 `records`、`list`、`items`、`data`。如果 `data` 本身是数组，也可以直接返回数组。

### 1.4 时间格式

时间字段使用 ISO-8601 本地时间字符串：

```text
2026-05-31T12:00:00
```

前端会用 `LocalDateTime.parse` 解析。

## 2. 用户与认证

### 2.1 普通用户注册

`POST /users/register`

请求体：

```json
{
  "username": "alice",
  "password": "Alice123"
}
```

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

### 2.2 管理员注册

`POST /users/register?inviteCode=123456`

请求体：

```json
{
  "username": "admin2",
  "password": "Admin123"
}
```

成功响应同普通注册。管理员邀请码由前端作为 query 参数传递。

### 2.3 登录

`POST /users/login`

请求体：

```json
{
  "username": "alice",
  "password": "Alice123"
}
```

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "userInfo": {
      "id": 1,
      "username": "alice",
      "role": "USER"
    }
  }
}
```

前端读取：

- 用户 ID：`data.userInfo.id` 或 `data.userInfo.userId`
- 用户名：`data.userInfo.username` 或 `data.userInfo.name`
- 角色：`data.userInfo.role`、`data.userInfo.userRole` 或 `data.role`
- 角色值：`USER` / `ADMIN`

如果用户被封禁，后端应返回非 200 状态并带解封时间：

```json
{
  "code": 403,
  "message": "该用户已被封禁",
  "data": {
    "bannedUntil": "2026-06-02T12:00:00"
  }
}
```

前端会显示：`您已被封禁，解封时间是 2026-06-02T12:00:00`。解封时间字段兼容 `bannedUntil`、`banExpireTime`、`until`、`unbanTime`。

### 2.4 修改个人信息

优先调用：

`PUT /users/profile`

请求体：

```json
{
  "currentUsername": "alice",
  "newUsername": "alice2",
  "newPassword": "Alice234"
}
```

如果该接口返回空数据，前端会尝试备用接口：

`PUT /users/me`

请求体：

```json
{
  "username": "alice",
  "newUsername": "alice2",
  "newPassword": "Alice234"
}
```

## 3. 图书

### 3.1 图书列表

`GET /books?page=1&size=10&sort=default&keyword=三体&title=三体&tags=科幻&tags=宇宙`

图书列表、普通用户图书搜索、管理员图书管理都使用该接口。后端应按查询条件、排序、分页参数返回当前页数据。

后端必须在数据库/服务端完成搜索、标签过滤、排序和分页。处理顺序建议为：

1. 按 `keyword` 或 `title` 过滤书名。
2. 按 `tags` 过滤标签。
3. 按 `sort` 排序。
4. 按 `page` 和 `size` 截取当前页。

不能先分页再过滤，否则前端页码、总数和搜索结果会不准确。

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `page` | 是 | 页码，从 1 开始 |
| `size` | 是 | 每页数量 |
| `sort` | 否 | 排序，见下方排序参数 |
| `keyword` | 否 | 书名关键字，按书名模糊搜索 |
| `title` | 否 | 书名关键字，含义同 `keyword` |
| `tags` | 否 | 标签，可重复传多个；多个标签按 AND 语义过滤，即图书必须同时包含所有传入标签 |

`keyword` 和 `title` 可能同时出现，后端按其中任意一个非空值处理即可；如果两者都非空且值相同，不要重复叠加条件。

排序参数：

- `default`：默认排序，建议按创建时间或 ID 倒序/正序保持稳定。
- `rating_desc`：按平均评分从高到低排序；评分相同建议用书名或 ID 做稳定次序。
- `title_asc`：按书名升序排序。

请求示例：

```text
GET /books?page=1&size=10&sort=default
GET /books?page=1&size=10&sort=default&keyword=三体&title=三体
GET /books?page=1&size=10&sort=default&tags=科幻&tags=宇宙
GET /books?page=2&size=10&sort=rating_desc&keyword=三体&title=三体&tags=科幻
```

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "id": 101,
        "title": "三体",
        "author": "刘慈欣",
        "description": "科幻小说",
        "tags": ["科幻", "宇宙"],
        "ratings": {
          "avgScore": 9.5,
          "ratingCount": 20,
          "distribution": {
            "1": 0,
            "2": 0,
            "3": 0,
            "4": 0,
            "5": 1,
            "6": 1,
            "7": 2,
            "8": 3,
            "9": 5,
            "10": 8
          }
        }
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 10
  }
}
```

前端读取图书卡片字段：

- ID：`id`、`bookId`、`book_id`
- 标题：`title`、`bookTitle`
- 作者：`author`、`bookAuthor`
- 标签：`tags`、`tagNames`、`tagList`，或 `tagsSummary` / `tags_summary`
- 评分：顶层字段或嵌套 `ratings` / `ratingSummary`

如果图书字段嵌套在 `bookInfo`、`book`、`data` 内，前端也会读取。

分页字段要求：

- `records`：当前页图书，不是全部图书。
- `total`：过滤后的总图书数量。
- `pages`：过滤后的总页数。
- `current`：当前页码，应等于请求中的 `page`，除非后端做了越界修正。
- `size`：当前页大小，应等于请求中的 `size`。

### 3.2 按标签搜索图书

`GET /books/search-by-tags?tags=科幻&tags=宇宙&sort=default&page=1&size=1000`

响应格式同图书列表。

该接口现在作为兼容接口保留。推荐后端优先在 `GET /books` 中直接支持 `tags`、`keyword/title`、`sort`、`page`、`size`，前端会优先按 `GET /books` 的分页结果展示。

### 3.3 图书详情

`GET /books/{bookId}`

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "bookInfo": {
      "id": 101,
      "title": "三体",
      "author": "刘慈欣",
      "description": "科幻小说",
      "tags": ["科幻", "宇宙"]
    },
    "ratings": {
      "avgScore": 9.5,
      "ratingCount": 20,
      "distribution": {
        "1": 0,
        "2": 0,
        "3": 0,
        "4": 0,
        "5": 1,
        "6": 1,
        "7": 2,
        "8": 3,
        "9": 5,
        "10": 8
      }
    },
    "comments": [],
    "replies": [],
    "favoriteCount": 5
  }
}
```

前端读取：

- 图书：`bookInfo`、`book`、`data`
- 评分：`ratings`、`ratingSummary`
- 评论：`comments`、`commentList`
- 回复：`replies`、`replyList`
- 收藏数：`favoriteCount`、`favoritesCount`、`collectCount`、`collectionCount`、`collectedCount`

如果详情响应中缺少评论，前端会调用 `GET /comments/book/{bookId}`。

如果缺少标签，前端会调用 `GET /books/{bookId}/tags`。

如果缺少评分，前端会调用 `GET /ratings/book/{bookId}/stats`。

如果收藏数字段为空或为 0，前端会通过收藏列表估算收藏数。

### 3.4 图书标签

`GET /books/{bookId}/tags`

成功响应可以是：

```json
{
  "code": 200,
  "message": "OK",
  "data": ["科幻", "宇宙"]
}
```

也可以是：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "tags": ["科幻", "宇宙"]
  }
}
```

前端兼容字段：`tags`、`tagList`、`tagNames`。

### 3.5 新增图书

`POST /books`

请求体：

```json
{
  "title": "三体",
  "author": "刘慈欣",
  "description": "地球文明与三体文明的接触与冲突。",
  "cover": "",
  "tags": ["科幻", "宇宙"]
}
```

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "book": {
      "id": 101,
      "title": "三体",
      "author": "刘慈欣",
      "description": "地球文明与三体文明的接触与冲突。",
      "tags": ["科幻", "宇宙"]
    }
  }
}
```

前端读取新增结果：`book`、`data`、`bookInfo`。

### 3.6 修改图书

`PUT /books/{bookId}`

请求体：

```json
{
  "title": "三体",
  "author": "刘慈欣",
  "description": "新版简介",
  "cover": "",
  "tags": ["科幻", "宇宙"]
}
```

成功响应：通用成功响应即可。

### 3.7 删除图书

`DELETE /books/{bookId}`

成功响应：通用成功响应即可。

## 4. 评分

### 4.1 给图书评分

`POST /ratings?bookId=101&score=9`

参数：

| 参数 | 说明 |
| --- | --- |
| bookId | 图书 ID |
| score | 评分，前端使用 1 到 10 |

成功响应：通用成功响应即可。

### 4.2 图书评分统计

`GET /ratings/book/{bookId}/stats`

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "avgScore": 9.5,
    "ratingCount": 20,
    "distribution": {
      "1": 0,
      "2": 0,
      "3": 0,
      "4": 0,
      "5": 1,
      "6": 1,
      "7": 2,
      "8": 3,
      "9": 5,
      "10": 8
    }
  }
}
```

前端兼容：

- 平均分：`average`、`avg`、`avgScore`、`averageScore`
- 评分数：`count`、`ratingCount`、`total`
- 分布：`distribution`、`ratingDistribution`

### 4.3 我的评分

`GET /ratings/my?bookId=101`

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "score": 9
  }
}
```

前端兼容字段：`score`、`myScore`、`data`。

## 5. 评论与投票

### 5.1 查询图书评论

`GET /comments/book/{bookId}?page=1&size=100`

成功响应：

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
      }
    ]
  }
}
```

前端读取评论字段：

- ID：`id`、`commentId`
- 用户：`username`、`user`、`nickname`
- 内容：`content`、`text`
- 时间：`createTime`、`time`、`createdAt`
- 父评论：`parentId`、`replyTo`
- 点赞数：`upVotes`、`upvoteCount`、`likes`
- 点踩数：`downVotes`、`downvoteCount`、`dislikes`

### 5.2 发表评论或回复

`POST /comments?bookId=101&content=内容&parentCommentId=501`

参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| bookId | 是 | 图书 ID |
| content | 是 | 评论内容 |
| parentCommentId | 否 | 回复某条评论时传 |

请求体：无，前端发送空 JSON。

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "comment": {
      "id": 502,
      "username": "alice",
      "content": "回复内容",
      "createTime": "2026-05-31T12:05:00",
      "parentId": 501,
      "upVotes": 0,
      "downVotes": 0
    }
  }
}
```

前端读取新增评论：`comment` 或 `data`。

### 5.3 删除自己的评论

`DELETE /comments/{commentId}`

成功响应：通用成功响应即可。

### 5.4 管理员删除评论

`DELETE /comments/admin/{commentId}`

成功响应：通用成功响应即可。

### 5.5 评论点赞/点踩

`POST /comments/{commentId}/votes?target=1`

参数：

| target | 说明 |
| --- | --- |
| `1` | 点赞 |
| `-1` | 点踩 |
| `0` | 取消投票 |

成功响应：通用成功响应即可。

### 5.6 管理员查询全站评论

`GET /comments/admin?page=1&size=1000`

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "id": 501,
        "username": "alice",
        "bookId": 101,
        "bookTitle": "三体",
        "content": "这本书不错",
        "createTime": "2026-05-31T12:00:00",
        "parentId": null,
        "upVotes": 3,
        "downVotes": 0
      },
      {
        "id": 502,
        "username": "bob",
        "bookInfo": {
          "id": 101,
          "title": "三体"
        },
        "content": "同意",
        "createTime": "2026-05-31T12:05:00",
        "parentCommentId": 501,
        "upVotes": 1,
        "downVotes": 0
      }
    ],
    "total": 2,
    "pages": 1,
    "current": 1,
    "size": 1000
  }
}
```

前端管理员评论列表展示：`所属图书 | 评论时间 | 一级评论/回复关系 | 用户与内容`，不展示评论 ID。

后端应返回：

- 所属图书：推荐 `bookId`、`bookTitle`；也兼容 `bookInfo.id`、`bookInfo.title`
- 评论时间：`createTime`、`time`、`createdAt`
- 父评论：`parentId`、`parentCommentId`、`replyTo`；为空表示一级评论，非空表示回复

其他评论字段兼容规则同 `5.1 查询图书评论`。

## 6. 收藏

### 6.1 查询我的收藏

`GET /collections?page=1&size=1000`

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "bookId": 101,
        "collectedAt": "2026-05-31T12:00:00",
        "bookInfo": {
          "id": 101,
          "title": "三体",
          "author": "刘慈欣",
          "tags": ["科幻"]
        },
        "ratings": {
          "avgScore": 9.5,
          "ratingCount": 20
        }
      }
    ]
  }
}
```

前端会把每条收藏记录解析成图书卡片，图书信息可在顶层，也可放在 `bookInfo`、`book`、`data` 中。
收藏页展示收藏时间，后端应在每条收藏记录中返回收藏时间。

前端读取收藏时间字段：

- 推荐字段：`collectedAt`
- 兼容字段：`collectTime`、`collectionTime`、`createTime`、`createdAt`、`time`

收藏页的搜索、分页、取消收藏：

- 搜索收藏：前端当前基于已加载收藏做书名/标签本地搜索。
- 收藏分页：前端当前基于已加载收藏做本地分页，每页 10 条。
- 取消收藏：前端调用 `DELETE /collections?bookId=...` 后刷新收藏列表。

### 6.2 检查是否已收藏

`GET /collections/check?bookId=101`

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "collected": true
  }
}
```

前端兼容字段：`data`、`result`、`value`、`collected`。可以直接返回布尔值，也可以返回对象。

### 6.3 收藏图书

`POST /collections?bookId=101`

成功响应：通用成功响应即可。

### 6.4 取消收藏

`DELETE /collections?bookId=101`

成功响应：通用成功响应即可。

## 7. 书单

### 7.1 查询书单列表

`GET /book-lists?page=1&size=1000`

前端会拉取全部书单后按 `owner` 或 `username` 在本地筛选当前用户。

成功响应：

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
        "bookIds": [101, 102],
        "publicVisible": true,
        "visibility": "PUBLIC"
      }
    ]
  }
}
```

前端读取书单字段：

- ID：`id`、`listId`
- 所有者：`owner`、`username`、`user`
- 标题：`title`
- 简介：`description`、`intro`
- 图书 ID 列表：`bookIds`、`bookIdList`、`books`
- 是否公开：`publicVisible`、`isPublic`、`public`、`visible`
- 可见性枚举：`visibility`、`privacy`、`mode`，值为 `PUBLIC` 或 `PRIVATE`

如果缺少可见性字段，前端按公开处理。

### 7.2 创建书单

`POST /book-lists?title=科幻精选&description=公开书单&publicVisible=true&visibility=PUBLIC`

请求体：无，前端发送空 JSON。

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

### 7.3 查询书单详情

`GET /book-lists/{listId}`

响应字段同书单对象。

非所有者只能查看公开书单，访问私密书单时建议后端返回 `403`。
前端打开他人的公开书单时会以只读模式展示，可以查看书单里的图书并打开图书详情，但不能添加、移除图书或修改可见性。

### 7.4 修改书单可见性

`PUT /book-lists/{listId}/visibility?publicVisible=false&visibility=PRIVATE`

只有书单所有者可修改。`publicVisible=true` 表示公开，`false` 表示私密。

### 7.5 删除书单

`DELETE /book-lists/{listId}`

成功响应：通用成功响应即可。

### 7.6 添加图书到书单

`POST /book-lists/{listId}/books?bookId=101`

或从书单详情页按书名添加：

`POST /book-lists/{listId}/books?bookTitle=三体`

图书详情页加入书单时，前端已知当前图书 ID，会使用 `bookId`。书单详情页用户输入图书名称，前端会优先使用 `bookTitle`；如果后端不支持，前端会尝试解析为 `bookId` 后调用旧接口。

成功响应：通用成功响应即可。

### 7.7 从书单移除图书

`DELETE /book-lists/{listId}/books?bookId=101`

成功响应：通用成功响应即可。

## 8. 用户主页

### 8.1 查询用户主页

`GET /users/profile/{username}`

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "userInfo": {
      "id": 2,
      "username": "bob"
    },
    "followingCount": 3,
    "followerCount": 5,
    "followedByCurrentUser": true,
    "comments": [
      {
        "id": 501,
        "username": "bob",
        "bookId": 101,
        "bookTitle": "三体",
        "content": "这本书不错",
        "createTime": "2026-05-31T12:00:00"
      }
    ],
    "bookLists": [
      {
        "id": 801,
        "owner": "bob",
        "title": "科幻精选",
        "description": "公开书单",
        "bookIds": [101],
        "publicVisible": true,
        "visibility": "PUBLIC"
      }
    ]
  }
}
```

前端读取：

- 用户信息：`userInfo`、`user`、`profile`
- 用户名：`username`、`user`、`name`
- 关注数：`followingCount`、`following`
- 粉丝数：`followerCount`、`followers`
- 当前用户是否已关注：`followedByCurrentUser`、`followed`
- 评论：`comments`、`commentList`、`records`
- 用户主页评论所属图书：推荐返回 `bookTitle` 和 `bookId`；前端也兼容嵌套 `bookInfo.title` / `bookInfo.id`
- 书单：`bookLists`、`lists`、`records`

用户主页中的书单需要按当前登录用户过滤：查看自己主页返回公开和私密书单；查看他人主页只返回公开书单。
前端支持在用户主页双击书单进入详情；自己的书单可编辑，他人的公开书单仅可查看。
用户主页评论列表展示“评论的是哪本书 + 评论内容”，不再展示评论时间；后端应在评论记录中返回图书标题，否则前端只能退回显示图书 ID。

### 8.2 用户名解析为用户 ID

前端在关注、私信、查看他人关注列表时会尝试解析用户 ID，顺序如下：

1. `GET /users/profile/{username}`
2. `GET /users/by-username/{username}`
3. `GET /users/{username}`

任一接口返回 `id` 或 `userId` 即可：

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

也可以嵌套在 `userInfo`、`user`、`data` 中。

## 9. 关注与粉丝

### 9.1 关注用户

`POST /follows?followeeId=2`

如果无法解析用户 ID，前端会使用：

`POST /follows?followeeUsername=bob`

成功响应：通用成功响应即可。

### 9.2 取消关注

`DELETE /follows?followeeId=2`

或：

`DELETE /follows?followeeUsername=bob`

成功响应：通用成功响应即可。

### 9.3 查询当前登录用户的关注列表

`GET /follows/followees?page=1&size=1000`

### 9.4 查询当前登录用户的粉丝列表

`GET /follows/followers?page=1&size=1000`

### 9.5 查询指定用户的关注列表

`GET /follows/user/{userId}/followees?page=1&size=1000`

### 9.6 查询指定用户的粉丝列表

`GET /follows/user/{userId}/followers?page=1&size=1000`

列表响应示例：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "followee": {
          "id": 2,
          "username": "bob"
        }
      }
    ]
  }
}
```

前端解析用户名兼容：

- 顶层：`username`、`user`、`followeeUsername`、`followerUsername`、`nickname`、`name`
- 嵌套：`userInfo`、`userInfoVO`、`user`、`followee`、`follower`、`profile`

## 10. 私信

### 10.1 发送私信

`POST /messages?receiverId=2&content=你好`

如果无法解析用户 ID，前端会使用：

`POST /messages?receiverUsername=bob&content=你好`

请求体：无，前端发送空 JSON。

成功响应：通用成功响应即可。

### 10.2 会话列表

`GET /messages/conversations`

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "peer": "bob",
        "lastMessage": "你好",
        "lastTime": "2026-05-31T12:00:00"
      }
    ]
  }
}
```

前端读取：

- 对方用户名：`peer`、`username`、`user`、`peerUsername`
- 最后一条消息：`lastMessage`、`content`、`message`
- 最后时间：`lastTime`、`time`、`updatedAt`

### 10.3 查询会话消息

`GET /messages/conversation/{peerIdOrUsername}?page=1&size=100`

如果能解析用户 ID，`peerIdOrUsername` 为用户 ID；否则为用户名。

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "from": "alice",
        "to": "bob",
        "content": "你好",
        "time": "2026-05-31T12:00:00"
      }
    ]
  }
}
```

前端读取：

- 发送方：`from`、`sender`、`senderUsername`
- 接收方：`to`、`receiver`、`receiverUsername`
- 内容：`content`、`message`
- 时间：`time`、`createTime`、`createdAt`

## 11. 管理员用户管理

### 11.1 查询封禁列表

`GET /users/bans?page=1&size=1000`

成功响应：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "username": "alice",
        "bannedUntil": "2026-06-01T12:00:00"
      }
    ]
  }
}
```

前端读取：

- 用户名：`username`、`user`
- 封禁截止时间：`bannedUntil`、`banExpireTime`、`until`、`unbanTime`

### 11.2 封禁用户

`POST /users/ban?username=alice&until=2026-06-01T12:00:00`

前端管理员页面输入封禁小时数后，会计算出 `until` 传给后端。`until` 为 ISO-8601 本地时间字符串。

成功响应：通用成功响应即可。

### 11.3 解封用户

`POST /users/unban?username=alice`

成功响应：通用成功响应即可。

前端支持从封禁列表选中用户后直接解除封禁。

## 12. 前端实际调用接口清单

| 模块 | 方法 | 路径 |
| --- | --- | --- |
| 用户 | POST | `/users/register` |
| 用户 | POST | `/users/register?inviteCode=...` |
| 用户 | POST | `/users/login` |
| 用户 | PUT | `/users/profile` |
| 用户 | PUT | `/users/me` |
| 用户 | GET | `/users/profile/{username}` |
| 用户 | GET | `/users/by-username/{username}` |
| 用户 | GET | `/users/{username}` |
| 图书 | GET | `/books?page=1&size=1000&sort=...` |
| 图书 | GET | `/books/search-by-tags?tags=...&sort=...&page=1&size=1000` |
| 图书 | GET | `/books/{bookId}` |
| 图书 | GET | `/books/{bookId}/tags` |
| 图书 | POST | `/books` |
| 图书 | PUT | `/books/{bookId}` |
| 图书 | DELETE | `/books/{bookId}` |
| 评分 | POST | `/ratings?bookId=...&score=...` |
| 评分 | GET | `/ratings/book/{bookId}/stats` |
| 评分 | GET | `/ratings/my?bookId=...` |
| 评论 | GET | `/comments/book/{bookId}?page=1&size=100` |
| 评论 | POST | `/comments?bookId=...&content=...&parentCommentId=...` |
| 评论 | DELETE | `/comments/{commentId}` |
| 评论 | DELETE | `/comments/admin/{commentId}` |
| 评论 | POST | `/comments/{commentId}/votes?target=...` |
| 评论 | GET | `/comments/admin?page=1&size=1000` |
| 收藏 | GET | `/collections?page=1&size=1000` |
| 收藏 | GET | `/collections/check?bookId=...` |
| 收藏 | POST | `/collections?bookId=...` |
| 收藏 | DELETE | `/collections?bookId=...` |
| 书单 | GET | `/book-lists?page=1&size=1000` |
| 书单 | POST | `/book-lists?title=...&description=...&publicVisible=...&visibility=...` |
| 书单 | GET | `/book-lists/{listId}` |
| 书单 | PUT | `/book-lists/{listId}/visibility?publicVisible=...&visibility=...` |
| 书单 | DELETE | `/book-lists/{listId}` |
| 书单 | POST | `/book-lists/{listId}/books?bookId=...` |
| 书单 | POST | `/book-lists/{listId}/books?bookTitle=...` |
| 书单 | DELETE | `/book-lists/{listId}/books?bookId=...` |
| 关注 | POST | `/follows?followeeId=...` |
| 关注 | POST | `/follows?followeeUsername=...` |
| 关注 | DELETE | `/follows?followeeId=...` |
| 关注 | DELETE | `/follows?followeeUsername=...` |
| 关注 | GET | `/follows/followees?page=1&size=1000` |
| 关注 | GET | `/follows/followers?page=1&size=1000` |
| 关注 | GET | `/follows/user/{userId}/followees?page=1&size=1000` |
| 关注 | GET | `/follows/user/{userId}/followers?page=1&size=1000` |
| 私信 | POST | `/messages?receiverId=...&content=...` |
| 私信 | POST | `/messages?receiverUsername=...&content=...` |
| 私信 | GET | `/messages/conversations` |
| 私信 | GET | `/messages/conversation/{peerIdOrUsername}?page=1&size=100` |
| 管理员 | GET | `/users/bans?page=1&size=1000` |
| 管理员 | POST | `/users/ban?username=...&until=...` |
| 管理员 | POST | `/users/unban?username=...` |
