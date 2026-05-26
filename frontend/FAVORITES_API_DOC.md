# 收藏图书查询接口说明（前端给后端对齐用）

> 目标：让“我的收藏”页面能正确显示当前登录用户收藏的图书。

## 1. 前端当前怎么查收藏

前端在 `FavoritesController.onShow()` 中调用：

```java
context.service().listFavorites(context.username());
```

远程实现里最终会请求：

```http
GET /api/collections?page=1&size=1000
```

### 说明
- 前端**不在 URL 里传用户名**，默认使用当前登录态（Cookie / Session）识别当前用户。
- 所以后端应当按**当前会话用户**返回“我的收藏”。
- 如果后端仍然返回全站收藏列表，前端需要额外过滤；但推荐后端直接返回当前用户自己的收藏。

---

## 2. 推荐的接口返回格式

建议统一为：

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

### 前端会优先读取
- `data.records`
- 若没有 `records`，也兼容 `list / items / rows / data`

---

## 3. 收藏列表接口

### 请求
```http
GET /api/collections?page=1&size=1000
```

### 请求头
```http
Cookie: <session cookie>
```

### 推荐返回
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

---

## 4. 前端解析收藏记录时能接受的字段

### 4.1 图书字段
前端对收藏项会尽量读取以下字段：
- `id` / `bookId` / `book_id`
- `title` / `bookTitle`
- `author` / `bookAuthor`
- `tags` / `tagNames` / `tagList`
- `tagsSummary` / `tags_summary`
- `averageScore`
- `ratings.avgScore`
- `ratings.ratingCount`

### 4.2 收藏记录字段
如果后端不是直接返回图书卡片，也可以返回收藏关系记录，前端兼容读取：
- `owner`
- `username`
- `user`
- `bookInfo`
- `book`
- `data`
- `bookId`

只要能最终解析出图书的：
- `id`
- `title`
- `author`
- `tags`
- `评分信息`

页面就能正常显示。

---

## 5. 评分字段建议

收藏页图书卡片右侧要显示平均分，建议后端返回：

```json
{
  "avgScore": 9.5,
  "ratingCount": 2
}
```

也兼容：
- `average`
- `avg`
- `averageScore`
- `count`
- `ratingCount`
- `total`

如果没有评分：
- 可以返回 `avgScore = 0`
- 或者不返回该字段，前端会显示为“暂无评分”或默认值

---

## 6. 当前接口对后端的最简要求

后端只要保证下面 4 点就能正常展示收藏页：

1. **按当前登录用户的 Session 返回收藏数据**
2. **分页结构包含 `records`**
3. **每条收藏记录能还原出图书 `id/title/author/tags`**
4. **最好带上评分字段 `avgScore` / `ratingCount`**

---

## 7. 建议的最小实现方式

如果后端已经有“收藏关系表”，推荐直接返回以下其中一种结构：

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

### 方案 B：返回收藏记录 + bookInfo
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

两种都可以，前端都能兼容。

---

## 8. 常见问题

### 8.1 为什么前端收藏页会空白？
通常是以下几种原因：
- `records` 字段没有返回
- 返回的是别的字段名，如 `items` 但结构不兼容
- `bookId` / `id` 丢了，前端无法还原图书
- `title` / `author` / `tags` 缺失
- Session 没有识别到当前用户，返回了空列表

### 8.2 为什么平均分显示成 0.0？
- 后端只返回了图书信息，没有返回评分字段
- 返回了评分字段，但字段名不是前端可识别的别名

推荐返回：
- `ratings.avgScore`
- `ratings.ratingCount`

---

## 9. 结论

“我的收藏”页最关键的是：

- `GET /api/collections?page=1&size=1000`
- `code / message / data`
- `data.records`
- 记录里至少包含 `bookId` 或 `bookInfo`
- 图书卡片字段尽量带全：`id/title/author/tags/avgScore/ratingCount`

这样前端就可以稳定显示收藏图书列表。

