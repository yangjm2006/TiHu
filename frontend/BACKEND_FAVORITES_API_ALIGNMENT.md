# 后端收藏接口对齐文档

> 目的：让前后端对齐“收藏 / 取消收藏 / 是否已收藏 / 我的收藏列表”四个核心能力，避免 `integer overflow`、状态误判、列表空白和按钮文案不同步。

---

## 1. 前端当前依赖的收藏接口

前端目前会调用以下接口：

```http
POST   /api/collections?bookId={bookId}
DELETE /api/collections?bookId={bookId}
GET    /api/collections/check?bookId={bookId}
GET    /api/collections?page=1&size=1000
```

### 说明
- 前端**不在 URL 中传用户名**，默认由后端根据当前登录态 / Session / Cookie 识别当前用户。
- `bookId` 必须使用 **Long / long**。
- 如果后端仍然使用 `Integer / int`，很容易在收藏相关场景触发 `integer overflow`。

---

## 2. 前端页面行为

### 2.1 图书详情页
- 若当前图书**未收藏**，按钮显示：`收藏`
- 若当前图书**已收藏**，按钮显示：`取消收藏`
- 点击后成功提示：
  - 收藏成功：`收藏成功`
  - 取消收藏成功：`取消收藏成功`

### 2.2 我的收藏页
- 收藏页会加载当前用户的收藏列表
- 支持双击图书打开详情页
- 从收藏页进入详情页后，返回按钮应显示：`返回我的收藏`

---

## 3. 是否已收藏接口

### 请求
```http
GET /api/collections/check?bookId=101
```

### 请求说明
- `bookId`：图书 ID，必须为 `Long`
- 当前登录用户由后端会话识别，不需要前端额外传用户名

### 推荐返回
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "collected": true
  }
}
```

### 前端兼容的返回字段
前端会尝试从以下位置读取布尔值：
- `data`
- `result`
- `value`
- `collected`

也就是说，下面这些返回也可以兼容：

```json
{ "data": true }
```

```json
{ "result": true }
```

```json
{ "value": true }
```

```json
{ "collected": true }
```

---

## 4. 收藏接口

### 请求
```http
POST /api/collections?bookId=101
```

### 请求说明
- 表示当前登录用户收藏该图书
- `bookId` 必须为 `Long`
- 后端应当根据当前会话用户创建收藏关系

### 推荐返回
```json
{
  "code": 200,
  "message": "收藏成功",
  "data": null
}
```

或：

```json
{
  "code": 200,
  "message": "OK"
}
```

---

## 5. 取消收藏接口

### 请求
```http
DELETE /api/collections?bookId=101
```

### 请求说明
- 表示当前登录用户取消收藏该图书
- `bookId` 必须为 `Long`
- 后端应当根据当前会话用户删除对应收藏关系

### 推荐返回
```json
{
  "code": 200,
  "message": "取消收藏成功",
  "data": null
}
```

或：

```json
{
  "code": 200,
  "message": "OK"
}
```

---

## 6. 我的收藏列表接口

### 请求
```http
GET /api/collections?page=1&size=1000
```

### 请求说明
- 当前用户的收藏列表
- 前端默认依赖当前登录态，不传用户名
- 建议后端直接返回当前用户自己的收藏，而不是全站收藏

### 推荐返回结构
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "bookId": 101,
        "owner": "alice",
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
        }
      }
    ],
    "total": 1,
    "pages": 1,
    "current": 1,
    "size": 1000
  }
}
```

### 前端解析规则
前端会尽量兼容以下字段：
- 收藏记录：`owner` / `username` / `user` / `bookInfo` / `book` / `data` / `bookId`
- 图书字段：`id` / `bookId` / `book_id` / `title` / `bookTitle` / `author` / `bookAuthor` / `tags` / `tagNames` / `tagList`
- 评分字段：`averageScore` / `average` / `avg` / `avgScore` / `averageScore` / `ratingCount` / `count` / `total`

---

## 7. 收藏列表最小可用返回

后端如果不想返回完整结构，至少需要让前端能恢复图书卡片信息。

### 方案 A：直接返回图书卡片
```json
{
  "bookId": 101,
  "title": "三体",
  "author": "刘慈欣",
  "tags": ["科幻", "宇宙"],
  "avgScore": 9.5,
  "ratingCount": 2
}
```

### 方案 B：返回收藏关系 + bookInfo
```json
{
  "bookId": 101,
  "owner": "alice",
  "bookInfo": {
    "id": 101,
    "title": "三体",
    "author": "刘慈欣",
    "tags": ["科幻", "宇宙"]
  },
  "ratings": {
    "avgScore": 9.5,
    "ratingCount": 2
  }
}
```

---

## 8. 必须统一的字段类型

为了彻底避免 `integer overflow`，后端收藏相关字段建议统一为：

- `bookId: Long`
- `userId: Long`
- `currentUserId: Long`
- `book_id: BIGINT`
- `user_id: BIGINT`

### 强烈建议不要使用
- `Integer`
- `int`
- `Math.toIntExact(...)`
- `Integer.parseInt(...)`

特别是在：
- Controller 参数
- Service 方法签名
- DTO / VO
- Entity / Table 映射
- MyBatis / JPA 映射

---

## 9. 后端最容易出错的点

### 9.1 是否已收藏接口返回字段不一致
如果后端返回的是其他名字，而不是布尔值，前端会误判为“未收藏”。

推荐至少返回：
```json
{ "collected": true }
```

### 9.2 收藏列表没有 `records`
前端分页解析优先读取 `records`。
如果后端返回的是 `items` / `list` / `rows`，前端可能无法正确显示。

### 9.3 `bookId` 类型过小
只要收藏相关链路中任何一个地方把 `bookId` 当成 `int`，就可能出现溢出或找不到图书。

### 9.4 Session 未识别到当前用户
如果后端没有正确识别登录态，`check` 和 `collections` 接口可能返回空列表或错误状态。

---

## 10. 推荐的统一约定

建议后端收藏接口统一采用以下约定：

### 收藏状态判断
```http
GET /api/collections/check?bookId={bookId}
```
返回：
```json
{ "code": 200, "message": "OK", "data": { "collected": true } }
```

### 收藏
```http
POST /api/collections?bookId={bookId}
```
返回：
```json
{ "code": 200, "message": "收藏成功" }
```

### 取消收藏
```http
DELETE /api/collections?bookId={bookId}
```
返回：
```json
{ "code": 200, "message": "取消收藏成功" }
```

### 我的收藏列表
```http
GET /api/collections?page=1&size=1000
```
返回：
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": []
  }
}
```

---

## 11. 给后端的结论

只要后端做到这几点，前端收藏功能就能稳定工作：

1. `bookId` / `userId` 全部使用 `Long`
2. `GET /api/collections/check?bookId=...` 返回明确的布尔收藏状态
3. `GET /api/collections?page=1&size=1000` 返回 `records`
4. 收藏 / 取消收藏接口按当前登录态处理
5. 收藏记录里尽量带全 `bookInfo` / `ratings`

---

## 12. 当前前端已对齐的行为

- 已收藏图书：按钮显示 `取消收藏`
- 未收藏图书：按钮显示 `收藏`
- 点击按钮后会切换收藏状态
- 收藏页支持打开详情页
- 从收藏页返回详情时，返回按钮显示 `返回我的收藏`

---

## 13. 一句话版说明

> 前端收藏相关接口：`POST /api/collections?bookId=...`、`DELETE /api/collections?bookId=...`、`GET /api/collections/check?bookId=...`、`GET /api/collections?page=1&size=1000`；请后端统一使用 `Long` 类型的 `bookId/userId`，并按当前登录态返回当前用户的收藏状态和收藏列表。

