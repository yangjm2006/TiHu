# 收藏按钮 `integer overflow` 问题说明（给后端）

> 现象：前端点击“收藏 / 取消收藏”按钮时，仍可能出现 `integer overflow`。
> 结论：这通常不是前端按钮文案问题，而是后端在收藏相关接口或数据库字段上仍使用了 `Integer` / `int`，导致长整型主键在转换时溢出。

## 1. 需要后端优先检查的地方

### 1.1 收藏相关 API 的参数类型
收藏按钮会调用这些接口：

```http
POST   /api/collections?bookId=...
DELETE /api/collections?bookId=...
GET    /api/collections/check?bookId=...
GET    /api/collections?page=1&size=1000
```

请确保：
- `bookId` 使用 **Long / long**
- 不要在 Controller、Service、DAO、Entity 中把 `bookId` 写成 `Integer / int`

### 1.2 收藏关系表 / 中间表字段类型
如果后端有收藏关系表，请确保：
- `book_id` 是 `BIGINT`
- 用户相关 id（如 `user_id`）也尽量统一为 `BIGINT`

### 1.3 收藏状态判断
`GET /api/collections/check?bookId=...` 建议返回：

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "collected": true
  }
}
```

或者兼容：

```json
{
  "code": 200,
  "message": "OK",
  "data": true
}
```

### 1.4 收藏列表返回结构
`GET /api/collections?page=1&size=1000` 建议返回：

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
          "description": "...",
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
          }
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

## 2. 前端当前的行为

前端收藏页和详情页的逻辑是：

- 详情页点击收藏后，按钮文案切换为：
  - 未收藏 → `收藏`
  - 已收藏 → `取消收藏`
- 点击后成功提示切换为：
  - `收藏成功`
  - `取消收藏成功`

如果后端返回类型或字段不对，前端可能会：
- 收藏状态不刷新
- 按钮文案异常
- 直接报错

---

## 3. 建议后端排查点

请重点检查这些地方是否仍有 `int`：

- Controller 方法参数：`@RequestParam Integer bookId`
- Service 方法签名：`toggleFavorite(Integer bookId)`
- Entity / DTO：`bookId` 是否写成 `Integer`
- MyBatis / JPA 映射：`book_id` 是否映射为 `int`
- 任何 `Math.toIntExact(...)` 或 `Integer.parseInt(...)`

---

## 4. 推荐的统一约定

后端收藏相关字段统一使用：

- `bookId: Long`
- `userId: Long`
- `currentUserId: Long`

只要主键统一为 `Long`，这类 `integer overflow` 问题通常就会消失。

