# 鹈鹕（TiHu）图书分享交流平台 - 后端实现指南

## 项目概述

鹈鹕是一个图书分享交流平台的后端系统，提供完整的用户认证、图书管理、评分评论、社交关注、私信等功能。

**技术栈**：
- Spring Boot 4.0.6
- Java 21
- MySQL 8.0+
- MyBatis-Plus 3.5.16
- Sa-Token 1.45.0（权限认证）
- Redis（缓存和Session存储）

## 快速开始

### 1. 数据库初始化

```bash
# 使用 schema.sql 初始化数据库
mysql -u root -p tihu < schema.sql

# 或在 MySQL 客户端执行 schema.sql 中的 SQL 语句
```

### 2. 修改数据库连接配置

编辑 `src/main/resources/application-dev.yaml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tihu?...
    username: your_mysql_username
    password: your_mysql_password
```

### 3. 启动Redis服务

确保 Redis 服务已启动（默认 localhost:6379）

### 4. 启动应用

```bash
# 使用 Maven
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# 或直接编译运行
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

应用将在 `http://localhost:9090/api` 启动

## API 接口概览

### 核心模块

#### 1. **用户管理** (`/api/users`)
- `POST /register` - 用户注册
- `POST /login` - 用户登录
- `POST /logout` - 用户登出
- `GET /me` - 获取当前用户信息
- `GET /profile/{username}` - 获取用户主页信息
- `PUT /{id}/username` - 修改用户名
- `PUT /{id}/password` - 修改密码
- **管理员**: `POST /admin/{id}/ban` 封禁用户，`DELETE /admin/{id}/ban` 解封用户

#### 2. **图书管理** (`/api/books`)
- `GET /` - 获取图书列表（分页，按评分排序）
- `GET /search?keyword=xxx` - 按书名搜索
- `GET /search-by-tags?tags=tag1,tag2` - 按标签搜索（多标签AND）
- `GET /{id}` - 获取图书详情（含评分统计）
- **管理员**: `POST /` 新增，`PUT /{id}` 修改，`DELETE /{id}` 删除

#### 3. **评分系统** (`/api/ratings`)
- `POST ?bookId=xxx&score=8` - 提交/更新评分
- `GET /my?bookId=xxx` - 获取我的评分
- `GET /book/{bookId}/stats` - 获取评分统计（平均分、分布等）

#### 4. **评论与点赞踩** (`/api/comments`)
- `POST ?bookId=xxx&content=xxx&parentCommentId=?` - 发表评论/回复
- `GET /book/{bookId}?page=1&size=10` - 获取图书评论列表
- `DELETE /{id}` - 撤回评论
- `POST /{id}/like` - 点赞
- `POST /{id}/dislike` - 点踩
- `DELETE /{id}/like` - 取消点赞/踩
- **管理员**: `DELETE /admin/{id}` 删除评论

#### 5. **收藏管理** (`/api/collections`)
- `POST ?bookId=xxx` - 收藏图书
- `DELETE ?bookId=xxx` - 取消收藏
- `GET ?page=1&size=10` - 获取我的收藏列表
- `GET /check?bookId=xxx` - 检查是否已收藏

#### 6. **书单管理** (`/api/book-lists`)
- `POST ?title=xxx&description=xxx` - 创建书单
- `GET ?page=1&size=10` - 获取我的书单列表
- `GET /{id}` - 获取书单详情
- `POST /{id}/books?bookId=xxx` - 添加书到书单
- `DELETE /{id}/books?bookId=xxx` - 从书单移除书
- `DELETE /{id}` - 删除书单

#### 7. **关注系统** (`/api/follows`)
- `POST ?followeeId=xxx` - 关注用户
- `DELETE ?followeeId=xxx` - 取消关注
- `GET /followees?page=1&size=10` - 获取我的关注列表
- `GET /followers?page=1&size=10` - 获取我的粉丝列表
- `GET /user/{userId}/followees` - 获取某用户的关注列表
- `GET /user/{userId}/followers` - 获取某用户的粉丝列表
- `GET /check?followeeId=xxx` - 检查是否已关注

#### 8. **私信系统** (`/api/messages`)
- `POST ?receiverId=xxx&content=xxx` - 发送私信
- `GET /conversation/{userId}?page=1&size=10` - 获取对话历史
- `GET /conversations` - 获取对话列表

## 实现状态

### ✅ 已完成到生产可用状态
- [x] 用户注册/登录/登出（支持邀请码注册管理员）
- [x] 用户权限管理（普通用户/管理员）
- [x] 图书CRUD（含逻辑删除）
- [x] 评分系统（1-10分，支持修改）
- [x] 评论系统（两级结构：一级评论+二级回复）
- [x] 点赞踩系统（3态互斥）
- [x] 收藏系统
- [x] 书单管理
- [x] 关注系统
- [x] 私信系统
- [x] 管理员后台基础接口

### 🔧 框架已搭建但需完善
- [ ] 搜索功能优化（当前支持模糊查询，可添加倒排索引/ES）
- [ ] 评论列表的递归查询（需优化二级回复展示）
- [ ] 缓存策略完善（评分统计、热门书籍等）
- [ ] 错误处理细化（补充更多Http状态码）
- [ ] 事务管理（删除图书时的级联）
- [ ] 异步任务（私信实时推送等）

## 关键实现细节

### 1. 登录与鉴权

使用Sa-Token框架实现Session+Cookie方式：
```java
// 登录时调用
StpUtil.login(user.getId());
String token = StpUtil.getTokenValue();

// 前端保存token并在请求头携带
// Authorization: {token}

// 需要登录验证的接口
@GetMapping("/me")
public Result getCurrentUser() {
    Long userId = Long.parseLong(StpUtil.getLoginId().toString());
    // ...
}
```

### 2. 逻辑删除

使用MyBatis-Plus的`@TableLogic`注解实现：
```java
@TableLogic
private Integer isDeleted;
```

配置在`application-dev.yaml`：
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 3. 密码加密

使用BCrypt加密算法：
```java
// 存储
user.setPassword(BCrypt.hashpw(password));

// 验证
boolean valid = BCrypt.checkpw(inputPassword, storedHash);
```

### 4. 评分统计

支持动态计算评分统计信息：
```java
// 返回格式
{
  "avgScore": 8.5,
  "ratingCount": 234,
  "distribution": {
    "1": 2, "2": 1, ..., "10": 50
  }
}
```

### 5. 两级评论结构

通过`parentCommentId`字段实现：
- `parentCommentId = NULL` → 一级评论
- `parentCommentId = xxx` → 二级回复

## 待优化项目

### 性能优化
1. **搜索优化**：Book搜索可集成Elasticsearch
   ```java
   // 当前实现
   Page<Book> searchByTitle(String keyword, int pageNum, int pageSize)
   
   // 可扩展为
   Page<Book> searchByFullText(String keyword)
   ```

2. **缓存优化**：评分统计缓存
   ```java
   @Cacheable(value = "bookRatingStats", key = "#bookId")
   public Map<String, Object> getRatingStats(Long bookId)
   ```

3. **数据库优化**：
   - Book表title字段添加索引
   - Comment表添加复合索引(bookId, parentCommentId, createTime)
   - Rating表唯一约束验证

### 功能扩展

1. **用户主页**：展示用户的评论和书单
   ```java
   GET /api/users/{userId}/comments
   GET /api/users/{userId}/book-lists
   ```

2. **动态流**（V2）：基于关注的消息推送

3. **推荐系统**（V2）：基于评分和收藏的图书推荐

4. **标签聚合**：图书标签的整理和去重

## 常见问题

### Q1: 启动报错 "Failed to configure a DataSource"
**A**: 检查MySQL是否启动，数据库连接信息是否正确

### Q2: 登录后仍提示401
**A**: 
- 检查Cookie是否正确保存
- 检查Sa-Token token-name配置是否一致
- 验证token是否过期（默认24小时）

### Q3: 评论删除后数据如何恢复
**A**: 使用逻辑删除，数据库保留记录。可通过直接更新`is_deleted=0`恢复

### Q4: 如何实现推荐书籍
**A**: 在BookService中添加：
```java
public Page<Book> getRecommendedBooks(Long userId, int page, int size) {
    // 基于用户关注、收藏、评分的推荐逻辑
}
```

## 文件结构

```
src/main/java/com/tihu/backend/
├── controller/           # REST接口层
│   ├── UserController
│   ├── BookController
│   ├── CommentController
│   ├── RatingController
│   ├── CollectionController
│   ├── BookListController
│   ├── FollowController
│   └── MessageController
├── service/              # 业务逻辑层
│   ├── UserService
│   ├── BookService
│   ├── CommentService
│   ├── RatingService
│   ├── CollectionService
│   ├── BookListService
│   ├── FollowService
│   ├── MessageService
│   └── impl/             # 实现类
├── entity/               # 实体类
├── mapper/               # 数据访问层
├── config/               # 配置类
├── common/               # 通用类（Result、ApiException等）
└── handler/              # 拦截器、异常处理
```

## 下一步计划

1. **前端对接**：JavaFX客户端集成
2. **性能测试**：压力测试和优化
3. **安全加固**：输入验证、SQL注入防护
4. **监控告警**：应用健康检查
5. **容器化**：Docker部署脚本

---

**项目开始日期**：2026-05-20  
**开发框架版本**：Spring Boot 4.0.6, MyBatis-Plus 3.5.16, Sa-Token 1.45.0  
**数据库版本**：MySQL 8.0+  
**Java版本**：21

