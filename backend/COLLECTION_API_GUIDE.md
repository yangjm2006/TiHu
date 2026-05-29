# 收藏接口说明（后端对前端对齐）

> 目的：说明当前后端收藏功能的接口、参数格式、返回结构，以及取消收藏是否已实现。

---

## 1. 结论

### 1.1 已实现的收藏相关接口

后端已经提供以下接口：

- `POST /api/collections`：收藏图书
- `DELETE /api/collections`：取消收藏图书
- `GET /api/collections`：获取当前登录用户的收藏列表
- `GET /api/collections/check`：检查当前用户是否已收藏某本书

### 1.2 取消收藏功能

**已实现。**

取消收藏接口为：

```http
DELETE /api/collections?bookId=xxx
```

后端会按**当前登录用户**和 `bookId` 删除对应收藏记录。

---

## 2. 统一约定

- 统一前缀：`/api`
- 收藏接口依赖 **Session / Cookie 登录态**
- 收藏相关主键统一使用 `Long`
- 不要把 `bookId` 写成 `Integer`，避免前端出现 `integer overflow`

---

## 3. 收藏图书

### 请求

```http
POST /api/collections?bookId=101
```

### 请求头

```http
Cookie: <session cookie>
```

### 说明

- `bookId` 类型：`Long`
- 当前用户从登录态获取，不需要前端在 URL 中传用户名

### 返回

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

### 常见错误

- 409：已收藏过该图书
- 401：未登录

---

## 4. 取消收藏

### 请求

```http
DELETE /api/collections?bookId=101
```

### 请求头

```http
Cookie: <session cookie>
```

### 说明

- `bookId` 类型：`Long`
- 按当前登录用户删除自己的收藏记录
- 如果当前用户没有收藏该图书，接口也会正常返回成功

### 返回

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

## 5. 获取我的收藏列表

### 请求

```http
GET /api/collections?page=1&size=1000
```

### 请求头

```http
Cookie: <session cookie>
```

### 返回格式

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [],
    "total": 0,
    "pages": 0,
    "current": 1,
    "size": 1000
  }
}
```

### 单条收藏记录建议包含

- `bookId`
- `id`
- `owner`
- `username`
- `title`
- `author`
- `description`
- `tags`
- `averageScore`
- `avgScore`
- `ratingCount`
- `bookInfo`
- `ratings`

---

## 6. 检查是否已收藏

### 请求

```http
GET /api/collections/check?bookId=101
```

### 返回

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "collected": true
  }
}
```

前端也兼容直接返回布尔值，但推荐统一使用 `data.collected`。

---

## 7. integer overflow 检查结论

后端收藏链路中，以下字段已经使用 `Long`：

- `Collection.bookId`
- `Collection.userId`
- 收藏接口的 `bookId` 参数
- 当前登录用户 ID

因此，收藏功能当前**不应该**再因为主键类型转换产生 `integer overflow`。

如果前端仍然报这个错，请优先检查：

- 是否有旧代码把 `bookId` 写成了 `Integer`
- 是否前端请求参数被转成了 `int`
- 是否数据库字段类型不是 `BIGINT`

---

## 8. 后端实现位置

### Controller

- `src/main/java/com/tihu/backend/controller/CollectionController.java`

### Service

- `src/main/java/com/tihu/backend/service/CollectionService.java`
- `src/main/java/com/tihu/backend/service/impl/CollectionServiceImpl.java`

### 实体

- `src/main/java/com/tihu/backend/entity/Collection.java`

---

## 9. 最终给前端的最简接口清单

- `POST /api/collections?bookId=xxx`：收藏
- `DELETE /api/collections?bookId=xxx`：取消收藏
- `GET /api/collections?page=1&size=1000`：我的收藏列表
- `GET /api/collections/check?bookId=xxx`：是否已收藏

> 以上接口都依赖当前登录态，不需要前端额外传用户名。

