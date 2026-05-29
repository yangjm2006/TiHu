# 图书详情页收藏人数接口对齐文档

> 目的：让图书详情页能展示“收藏人数：xx人”，并与后端图书详情接口对齐。

---

## 1. 前端当前展示需求

图书详情页需要展示以下信息：
- 图书标题
- 作者
- 标签
- 简介
- 平均评分
- 评分人数
- **收藏人数**

其中“收藏人数”需要由后端接口返回，前端会直接展示为：

```text
收藏人数：xx人
```

---

## 2. 图书详情接口

### 请求
```http
GET /api/books/{bookId}
```

### 请求说明
- `bookId` 必须使用 `Long / long`
- 当前登录用户可用于返回 `myScore`、`collected` 等个性化信息

---

## 3. 推荐返回结构

建议后端在图书详情接口中返回：

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
    "favoriteCount": 12,
    "favoritesCount": 12,
    "collectCount": 12,
    "collectionCount": 12,
    "collectedCount": 12,
    "comments": [],
    "replies": []
  }
}
```

---

## 4. 前端会识别的收藏人数字段

前端详情页会优先读取以下字段名：
- `favoriteCount`
- `favoritesCount`
- `collectCount`
- `collectionCount`
- `collectedCount`

后端已统一对齐并返回这些别名，推荐前端优先读取 `favoriteCount`。

如果这些字段都没有，前端会退回到收藏列表做统计，但这会增加一次额外请求，所以推荐后端直接返回。

---

## 5. 收藏人数的含义

这里的“收藏人数”指的是：

> **收藏了这本书的不同用户数量**

不是收藏总次数，也不是收藏记录条数的重复累计。

例如：
- 用户 A 收藏了这本书
- 用户 B 收藏了这本书
- 用户 A 再次取消后又收藏

最终前端展示的收藏人数应是当前去重后的用户数。

---

## 6. 后端实现建议

### 6.1 数据来源
如果后端有收藏关系表，建议直接按 `bookId` 统计去重用户数。

### 6.2 字段类型
请统一使用：
- `bookId: Long`
- `userId: Long`
- `favoriteCount: Long` 或 `Integer`

> 但 `bookId` 和 `userId` 必须使用 `Long`，不要用 `int` / `Integer`。

### 6.3 SQL 示例
```sql
SELECT COUNT(DISTINCT user_id) AS favoriteCount
FROM book_collection
WHERE book_id = ?
```

---

## 7. 前端展示规则

前端会直接显示：

```text
收藏人数：12人
```

如果后端返回为 0：

```text
收藏人数：0人
```

---

## 8. 常见问题

### 8.1 为什么前端显示不出收藏人数？
通常是因为后端图书详情接口没有返回 `favoriteCount`，或者返回字段名不在兼容列表里。

### 8.2 为什么收藏人数不准确？
可能是后端统计了收藏记录总条数，但没有按用户去重。

### 8.3 为什么会 `integer overflow`？
通常是收藏表或接口参数仍然使用了 `int` / `Integer`，尤其是 `bookId` / `userId` 相关字段。

---

## 9. 后端最小对齐要求

后端只要做到下面 3 点，前端就能正常显示：

1. `GET /api/books/{bookId}` 返回收藏人数字段
2. 收藏人数字段命名为 `favoriteCount` 优先
3. `bookId` / `userId` 全部统一使用 `Long`

---

## 10. 一句话版说明

> 请在图书详情接口 `GET /api/books/{bookId}` 中补充收藏人数字段，推荐返回 `favoriteCount`，表示收藏该书的去重用户数；前端会展示为“收藏人数：xx人”，并兼容 `favoritesCount / collectCount / collectionCount / collectedCount` 这些别名。

