# 图书列表分页与排序接口对齐文档

> 目的：让前后端对齐“图书列表分页、返回详情后恢复原页、排序切换”这几个能力，避免返回第一页、搜索条件丢失、排序不一致。

---

## 1. 前端当前行为

### 1.1 图书列表分页
- 列表默认每页 10 本
- 用户从第几页打开图书详情，返回列表后应回到第几页
- 当前搜索词、标签筛选、排序方式都应保持不变

### 1.2 排序切换
图书列表右上角排序支持三种模式：
- `默认排序`
- `按评分排序`
- `按书名排序`

### 1.3 默认排序含义
- 默认排序 = 按图书增加顺序排序
- 也就是后端如果不传排序参数，尽量返回插入顺序 / 创建顺序

---

## 2. 图书列表接口

### 2.1 图书分页列表
```http
GET /api/books?page=1&size=10&sort=default
```

### 推荐查询参数
- `page`：当前页，从 1 开始
- `size`：每页条数
- `sort`：排序方式，可选值：
  - `default`：默认顺序 / 增加顺序
  - `rating_desc`：按评分从高到低
  - `title_asc`：按书名升序

### 返回说明

- 返回统一使用 `code / message / data`
- `data` 中分页字段统一为：`records / total / pages / current / size`
- 前端返回详情页后，会继续按当前页、当前排序、当前筛选条件重新请求列表

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

---

## 3. 标签筛选列表接口

如果后端把“按标签搜索”单独做成接口，建议也支持同样的排序参数：

```http
GET /api/books/search-by-tags?tags=科幻&tags=宇宙&page=1&size=10&sort=default
```

或者：

```http
GET /api/books/search-by-tags?tags=科幻 宇宙&page=1&size=10&sort=default
```

### 说明
- 前端会按标签 AND 关系筛选
- `sort` 参数建议和图书分页接口保持一致
- 标签筛选接口也应返回相同分页结构：`records / total / pages / current / size`

---

## 4. 排序字段建议

### 4.1 默认排序
- 按图书创建时间 / 入库顺序 / 插入顺序
- 建议后端保证稳定排序，不要每次请求顺序随机变化

### 4.2 按评分排序
- 按平均分从高到低
- 如果平均分相同，建议再按书名或 id 做稳定排序

### 4.3 按书名排序
- 按书名升序
- 如果书名相同，建议再按 id 做稳定排序

---

## 5. 推荐的最小返回字段

列表页卡片至少需要：
- `id`
- `title`
- `author`
- `tags`
- `averageScore`

示例：
```json
{
  "id": 101,
  "title": "三体",
  "author": "刘慈欣",
  "tags": ["科幻", "宇宙"],
  "averageScore": 9.5
}
```

---

## 6. 后端最容易出错的点

### 6.1 返回第一页导致状态丢失
前端从详情返回时，如果后端分页接口不稳定，页面容易看起来像“回到第一页”。

### 6.2 排序参数不支持
如果后端暂时不支持 `sort`，前端仍然可以做兜底排序，但建议后端尽快支持，以保证翻页一致性。

### 6.3 默认排序不稳定
如果默认顺序每次请求都变，用户会感觉“返回后列表乱了”。

---

## 7. 给后端的最简对齐要求

1. `GET /api/books?page=&size=&sort=` 支持分页和排序
2. `sort` 支持：`default` / `rating_desc` / `title_asc`
3. 默认排序尽量按图书增加顺序
4. 分页返回统一使用 `records / total / pages / current / size`
5. 标签筛选接口也尽量支持同样的 `sort`

## 8. 一句话版说明

> 前端图书列表支持分页状态保持与排序切换；后端请在 `GET /api/books` 和 `GET /api/books/search-by-tags` 中支持 `sort=default|rating_desc|title_asc`，并统一返回 `records / total / pages / current / size`。

---

## 8. 一句话版说明

> 前端图书列表新增了分页状态保持和排序切换；请后端在 `GET /api/books` 和（如有）`GET /api/books/search-by-tags` 中支持 `sort=default|rating_desc|title_asc`，其中 `default` 表示按图书增加顺序返回，并保持分页返回字段为 `records / total / pages / current / size`。

