 # TiHu V1 后端对齐说明（给后端直接实现）

> 目标：前端按下面这套格式联调，后端尽量严格对齐，避免字段名不一致导致页面空白、登录失败、分页解析失败。

---

## 1. 基本约定

- 统一前缀：`/api`
- 所有接口都返回 JSON
- 登录态使用：`Session + Cookie`
- 所有时间字段统一使用 **ISO-8601** 字符串
  - 示例：`2026-05-20T13:45:00`
- 登录/注册请求必须带：
  - `Content-Type: application/json; charset=UTF-8`

---

## 2. 统一响应格式

所有接口都尽量返回：

```json
{
  "code": 200,
  "message": "OK",
  "data": {}
}
```

### 约定
- `code = 200`：成功
- 其他值：失败
- 失败时也要有 `message`
- `data` 可为对象、数组、字符串、数字或 `null`

### 推荐失败格式

```json
{
  "code": 400,
  "message": "参数错误",
  "data": null
}
```

---

## 3. 分页接口格式

图书列表、收藏列表、书单列表、评论列表、关注列表等分页接口，**最推荐**返回：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [],
    "total": 0,
    "pages": 0,
    "current": 1,
    "size": 10
  }
}
```

### 重点字段
- `records`：列表数据
- `total`：总条数
- `pages`：总页数
- `current`：当前页
- `size`：每页条数

### 说明
- 前端优先读 `records`
- 如果你们后端历史原因只能返回 `items / list / rows / content`，也可以做兼容，但**最好统一成 `records`**

---

## 4. 登录与注册

### 4.1 普通用户注册

`POST /api/users/register`

请求体：

```json
{
  "username": "alice",
  "password": "Alice123"
}
```

### 4.2 管理员注册

`POST /api/users/register?inviteCode=123456`

请求体同上：

```json
{
  "username": "admin2",
  "password": "Admin123"
}
```

### 4.3 登录

`POST /api/users/login`

请求体：

```json
{
  "username": "alice",
  "password": "Alice123"
}
```

### 4.4 登录成功返回建议

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

### 登录态要求
- 必须返回 `Set-Cookie`
- 后续接口依赖 Cookie 维持 Session

---

## 5. 图书接口

### 5.1 图书分页列表

`GET /api/books?page=1&size=10`

### 推荐返回

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
        "tags": ["科幻", "宇宙"],
        "averageScore": 9.5
      }
    ],
    "total": 11,
    "pages": 2,
    "current": 1,
    "size": 10
  }
}
```

### 5.2 图书详情

`GET /api/books/{id}`

### 推荐返回

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "bookInfo": {
      "id": 101,
      "title": "三体",
      "author": "刘慈欣",
      "description": "地球文明与三体文明的接触与冲突。",
      "tags": ["科幻", "宇宙"]
    },
    "ratings": {
      "avgScore": 9.5,
      "ratingCount": 2,
      "distribution": {
        "1": 0,
        "2": 0,
        "3": 0,
        "4": 0,
        "5": 0,
        "6": 0,
        "7": 0,
        "8": 0,
        "9": 1,
        "10": 1
      },
      "myScore": 10
    },
    "comments": [],
    "replies": []
  }
}
```

### 图书建议字段
- `id`
- `title`
- `author`
- `description`
- `tags`：数组
- `averageScore`：数字

---

## 6. 评分接口

### 6.1 提交 / 更新评分

`POST /api/ratings?bookId=1&score=8`

### 6.2 获取我的评分

`GET /api/ratings/my?bookId=1`

### 6.3 获取评分统计

`GET /api/ratings/book/{bookId}/stats`

### 建议返回字段
- `avgScore`
- `ratingCount`
- `distribution`
- `myScore`

---

## 7. 评论接口

### 7.1 发表评论 / 回复

`POST /api/comments?bookId=1&content=很好看&parentCommentId=1001`

### 7.2 获取图书评论

`GET /api/comments/book/{bookId}?page=1&size=10`

### 7.3 删除自己的评论

`DELETE /api/comments/{commentId}`

### 7.4 管理员删除评论

`DELETE /api/comments/admin/{commentId}`

### 评论建议字段

```json
{
  "id": 1001,
  "user": "alice",
  "content": "世界观很震撼。",
  "time": "2026-05-25T13:45:00",
  "parentId": null,
  "upVotes": 3,
  "downVotes": 0
}
```

---

## 8. 收藏接口

### 8.1 收藏图书

`POST /api/collections?bookId=1`

### 8.2 取消收藏

`DELETE /api/collections?bookId=1`

### 8.3 检查是否收藏

`GET /api/collections/check?bookId=1`

### 8.4 我的收藏列表

`GET /api/collections?page=1&size=10`

---

## 9. 书单接口

### 9.1 创建书单

`POST /api/book-lists?title=我的书单&description=简介`

### 9.2 我的书单列表

`GET /api/book-lists?page=1&size=10`

### 9.3 书单详情

`GET /api/book-lists/{listId}`

### 9.4 向书单加书

`POST /api/book-lists/{listId}/books?bookId=1`

### 9.5 从书单移除图书

`DELETE /api/book-lists/{listId}/books?bookId=1`

### 9.6 删除书单

`DELETE /api/book-lists/{listId}`

### 书单建议字段

```json
{
  "id": 2001,
  "owner": "alice",
  "title": "科幻入门",
  "description": "适合刚接触科幻的读者",
  "bookIds": [101, 108]
}
```

---

## 10. 关注接口

### 10.1 关注用户

`POST /api/follows?followeeId=3`

### 10.2 取消关注

`DELETE /api/follows?followeeId=3`

### 10.3 我关注的人

`GET /api/follows/followees?page=1&size=10`

### 10.4 我的粉丝

`GET /api/follows/followers?page=1&size=10`

### 10.5 检查是否关注

`GET /api/follows/check?followeeId=3`

---

## 11. 私信接口

### 11.1 发送私信

`POST /api/messages?receiverId=3&content=你好`

### 11.2 会话列表

`GET /api/messages/conversations`

### 11.3 某会话全部历史消息

`GET /api/messages/conversation/{peerId}?page=1&size=100`

### 消息建议字段

```json
{
  "from": "alice",
  "to": "bob",
  "content": "你好",
  "time": "2026-05-25T13:45:00"
}
```

---

## 12. 时间格式

所有时间字段统一使用 ISO-8601：

```text
2026-05-25T13:45:00
```

不要返回：
- 时间戳数字
- 自定义字符串格式
- 毫秒整数

---

## 13. 给后端的最简对齐要求

请尽量严格对齐下面 4 点：

1. **统一返回格式**
   - `code / message / data`
   - 成功码：`200`

2. **分页格式统一**
   - `records / total / pages / current / size`

3. **登录注册 JSON 请求**
   - 请求体：`{ "username": "...", "password": "..." }`
   - 请求头：`Content-Type: application/json; charset=UTF-8`

4. **时间格式统一**
   - ISO-8601

---

## 14. 备注

- 如果你们后端历史原因不能完全按上述字段名返回，也可以加兼容字段
- 但前端最希望你们优先对齐上面的字段名和结构
- 这样最不容易出现“有数据但页面为空”这种问题

