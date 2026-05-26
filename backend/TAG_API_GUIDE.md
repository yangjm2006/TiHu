# 图书标签接口说明（前后端对齐）

> 目的：说明当前后端对“图书标签”的**接口要求、参数格式、数据库关系**，以及前端应该如何把标签正确写入数据库。

---

## 0. 本文档补充说明

- 前端已按“优先从图书详情读取 `tags`，必要时再调用独立标签接口补齐”的方式实现。
- 当前文档中的接口路径统一以 `/api` 为前缀，供后端对接时直接参考。
- 若后端暂未实现某个字段，前端会尽量兼容 `data / book / bookInfo / tags / tagList / tagNames / tagsSummary` 等常见返回结构。

---

## 1. 先说结论

### 1.1 当前后端已经支持“写入图书标签”

现在后端已经补齐了标签相关接口，支持：

- 新增标签
- 查询标签列表
- 创建/更新图书时同时写入标签
- 单独替换某本图书的标签

### 1.2 这套接口的统一约定

- 图书标签写入优先使用**标签名数组**：`tags: ["科幻", "宇宙"]`
- 后端会自动：
  1. 先查 `tag` 表
  2. 不存在则创建 `tag`
  3. 再写入 `book_tag`

### 1.3 当前后端支持的标签相关接口

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| GET | `/api/tags` | 获取标签列表 |
| POST | `/api/tags` | 创建标签（管理员） |
| GET | `/api/books/search-by-tags` | 按标签查询图书 |
| GET | `/api/books/{id}/tags` | 获取某本书的标签 |
| PUT | `/api/books/{id}/tags` | 替换某本书的标签（管理员） |

> `search-by-tags` 已支持多标签 AND 查询。

---

## 2. 当前接口要求的标签格式

### 2.1 查询接口参数

后端方法签名：

```java
@GetMapping("/search-by-tags")
public Result searchByTags(@RequestParam List<String> tags,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int size)
```

### 2.2 推荐前端传参方式

#### 方式 A：同名参数重复传递（推荐）

```text
/api/books/search-by-tags?tags=科幻&tags=宇宙&page=1&size=10
```

#### 方式 B：逗号分隔（与代码注释示例一致）

```text
/api/books/search-by-tags?tags=科幻,宇宙&page=1&size=10
```

> 说明：后端会把重复参数和逗号分隔都识别为标签数组。为了减少前后端转换差异，**推荐统一使用方式 A**。

### 2.3 返回格式

分页返回统一包装为：

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

---

## 3. 标签写入数据库的接口

### 3.1 现在有哪些写入接口

- `POST /api/tags`：创建标签（管理员）
- `PUT /api/books/{id}/tags`：替换某本书的标签（管理员）
- `POST /api/books`：创建图书时可同时写入 `tags`
- `PUT /api/books/{id}`：更新图书时可同时写入 `tags`

### 3.2 提交规则

- `tags` 传 **标签名数组**，例如：`["科幻", "宇宙"]`
- 后端会自动：先查 `tag`，不存在就创建，再写入 `book_tag`
- `tags: []` 表示清空该书原有标签
- 不传 `tags` 表示不修改标签

### 3.3 `Book` 实体已经增加 `tags` 字段

`Book` 实体新增了一个非数据库字段：

- `tags`: `List<String>`

原有核心字段仍然包括：

- `id`
- `title`
- `author`
- `isbn`
- `description`
- `cover`
- `publishDate`
- `avgRating`
- `ratingCount`
- `isDeleted`

所以前端现在在 `createBook` / `updateBook` 请求里直接带 `tags`，后端会自动保存。

---

## 4. 数据库里标签相关表结构

### 4.1 标签表：`tag`

数据库脚本：`src/main/resources/schema.sql`

字段：

- `id`
- `name`
- `description`
- `create_time`

### 4.2 图书-标签关联表：`book_tag`

字段：

- `id`
- `book_id`
- `tag_id`

这说明数据库设计是**多对多关系**：

- 一个图书可以有多个标签
- 一个标签也可以属于多本图书

### 4.3 当前写库逻辑实现方式

相关类：

- `Tag`
- `BookTag`
- `TagServiceImpl`
- `BookTagMapper`

现在后端会在保存图书标签时自动做这几步：

1. 创建标签（写 `tag` 表）
2. 绑定图书和标签（写 `book_tag` 表）
3. 更新图书时替换旧标签关系

---

## 5. 前端该怎么传

### 5.1 创建/更新图书时顺便保存标签（推荐）

接口：`POST /api/books` 或 `PUT /api/books/{id}`

请求体里直接带 `tags` 数组：

```json
{
  "title": "三体",
  "author": "刘慈欣",
  "description": "...",
  "tags": ["科幻", "宇宙"]
}
```

### 5.2 单独替换某本书的标签

```http
PUT /api/books/{id}/tags
```

请求体：

```json
{
  "tags": ["科幻", "宇宙"]
}
```

### 5.3 新增标签字典

```http
POST /api/tags
```

请求体：

```json
{
  "name": "科幻",
  "description": "科幻类图书"
}
```

---

## 6. 当前实现状态总结

### 已有

- 图书标签查询接口：`GET /api/books/search-by-tags`
- 图书标签读取接口：`GET /api/books/{id}/tags`
- 图书标签写入接口：`PUT /api/books/{id}/tags`
- 标签列表接口：`GET /api/tags`
- 标签新增接口：`POST /api/tags`
- 标签表：`tag`
- 图书-标签关联表：`book_tag`
- 标签相关实体和 Mapper

### 说明

- 查询接口在控制器层和服务层都已实现
- 创建/更新图书时可直接携带 `tags` 数组
- 单独替换标签时使用 `PUT /api/books/{id}/tags`
- 前端远程读取图书详情时，已增加对独立标签接口的兜底读取

### 未有

- 标签删除接口
- 标签编辑接口
- 标签排序接口

---

## 7. 最后一句话

如果你的目标是“前端上传标签后写入数据库”，现在后端已经支持：

- 创建/更新图书时直接带 `tags`
- 或者单独调用 `PUT /api/books/{id}/tags`

如果你的目标只是“按标签查书”，那前端请按 `tags=科幻&tags=宇宙` 这种格式请求 `GET /api/books/search-by-tags`。
