package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.entity.Book;
import com.tihu.backend.entity.BookList;
import com.tihu.backend.entity.BookListItem;
import com.tihu.backend.entity.User;
import com.tihu.backend.mapper.BookListItemMapper;
import com.tihu.backend.mapper.BookListMapper;
import com.tihu.backend.mapper.BookMapper;
import com.tihu.backend.mapper.UserMapper;
import com.tihu.backend.service.BookListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookListServiceImpl extends ServiceImpl<BookListMapper, BookList> implements BookListService {

    @Autowired
    private BookListItemMapper bookListItemMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public Object createBookList(Long userId, String title, String description, Boolean publicVisible, String visibility) throws Exception {
        if (!StringUtils.hasText(title)) {
            throw new ApiException(400, "书单标题不能为空");
        }

        BookList bookList = new BookList();
        bookList.setUserId(userId);
        bookList.setTitle(title.trim());
        bookList.setDescription(description);
        bookList.setIsPublic(resolvePublicVisible(publicVisible, visibility));
        this.save(bookList);

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> record = toBookListRecord(bookList);
        data.put("bookList", record);
        data.put("list", record);
        return data;
    }

    @Override
    public Page<Object> getUserBookLists(Long userId, int pageNum, int pageSize) {
        Page<BookList> page = this.page(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<BookList>().eq(BookList::getUserId, userId).orderByDesc(BookList::getCreateTime));

        return toBookListPage(page);
    }

    @Override
    public Page<Object> getVisibleBookLists(Long viewerUserId, int pageNum, int pageSize) {
        LambdaQueryWrapper<BookList> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(q -> q.eq(BookList::getUserId, viewerUserId)
                .or()
                .eq(BookList::getIsPublic, true)
                .or()
                .isNull(BookList::getIsPublic))
            .orderByDesc(BookList::getCreateTime);

        Page<BookList> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return toBookListPage(page);
    }

    @Override
    public Object getBookListDetail(Long bookListId, Long currentUserId) {
        BookList bookList = this.getById(bookListId);
        if (bookList == null) {
            throw new ApiException(404, "书单不存在");
        }
        if (!bookList.getUserId().equals(currentUserId) && !isPublic(bookList)) {
            throw new ApiException(403, "无权查看私密书单");
        }
        return toBookListRecord(bookList);
    }

    @Override
    @Transactional
    public void updateVisibility(Long bookListId, Long userId, Boolean publicVisible, String visibility) throws Exception {
        BookList bookList = requireOwnedBookList(bookListId, userId, "无权修改他人书单");
        bookList.setIsPublic(resolvePublicVisible(publicVisible, visibility));
        this.updateById(bookList);
    }

    @Override
    @Transactional
    public void addBookToList(Long bookListId, Long bookId, Long userId) throws Exception {
        requireOwnedBookList(bookListId, userId, "无权修改他人书单");

        Book book = bookMapper.selectById(bookId);
        if (book == null || Integer.valueOf(1).equals(book.getIsDeleted())) {
            throw new ApiException(404, "图书不存在");
        }

        LambdaQueryWrapper<BookListItem> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(BookListItem::getBookListId, bookListId).eq(BookListItem::getBookId, bookId);
        if (bookListItemMapper.selectCount(existingWrapper) > 0) {
            return;
        }

        Long count = bookListItemMapper.selectCount(new LambdaQueryWrapper<BookListItem>().eq(BookListItem::getBookListId, bookListId));
        BookListItem item = new BookListItem();
        item.setBookListId(bookListId);
        item.setBookId(bookId);
        item.setSortOrder(count.intValue());
        bookListItemMapper.insert(item);
    }

    @Override
    @Transactional
    public void addBookToListByTitle(Long bookListId, String bookTitle, Long userId) throws Exception {
        if (!StringUtils.hasText(bookTitle)) {
            throw new ApiException(400, "图书名称不能为空");
        }
        Book book = bookMapper.selectOne(new LambdaQueryWrapper<Book>()
            .eq(Book::getTitle, bookTitle.trim())
            .eq(Book::getIsDeleted, 0)
            .last("LIMIT 1"));
        if (book == null) {
            throw new ApiException(404, "图书不存在");
        }
        addBookToList(bookListId, book.getId(), userId);
    }

    @Override
    @Transactional
    public void removeBookFromList(Long bookListId, Long bookId, Long userId) throws Exception {
        requireOwnedBookList(bookListId, userId, "无权修改他人书单");
        bookListItemMapper.delete(new LambdaQueryWrapper<BookListItem>()
            .eq(BookListItem::getBookListId, bookListId)
            .eq(BookListItem::getBookId, bookId));
    }

    @Override
    @Transactional
    public void deleteBookList(Long bookListId, Long userId) throws Exception {
        requireOwnedBookList(bookListId, userId, "无权删除他人书单");
        bookListItemMapper.delete(new LambdaQueryWrapper<BookListItem>().eq(BookListItem::getBookListId, bookListId));
        boolean removed = this.removeById(bookListId);
        if (!removed) {
            throw new ApiException(404, "书单不存在");
        }
    }

    @Override
    public Object getProfileBookLists(Long profileUserId, Long viewerUserId) {
        LambdaQueryWrapper<BookList> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookList::getUserId, profileUserId);
        if (!profileUserId.equals(viewerUserId)) {
            wrapper.eq(BookList::getIsPublic, true);
        }
        wrapper.orderByDesc(BookList::getCreateTime);

        return this.list(wrapper).stream().map(this::toBookListRecord).toList();
    }

    private BookList requireOwnedBookList(Long bookListId, Long userId, String message) {
        BookList bookList = this.getById(bookListId);
        if (bookList == null) {
            throw new ApiException(404, "书单不存在");
        }
        if (!bookList.getUserId().equals(userId)) {
            throw new ApiException(403, message);
        }
        return bookList;
    }

    private Boolean resolvePublicVisible(Boolean publicVisible, String visibility) {
        if (publicVisible != null) {
            return publicVisible;
        }
        if (StringUtils.hasText(visibility)) {
            return !"PRIVATE".equalsIgnoreCase(visibility.trim()) && !"私密".equals(visibility.trim());
        }
        return true;
    }

    private boolean isPublic(BookList bookList) {
        return bookList.getIsPublic() == null || Boolean.TRUE.equals(bookList.getIsPublic());
    }

    private Page<Object> toBookListPage(Page<BookList> source) {
        Page<Object> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setPages(source.getPages());
        result.setRecords(source.getRecords().stream()
            .map(this::toBookListRecord)
            .<Object>map(record -> record)
            .toList());
        return result;
    }

    private Map<String, Object> toBookListRecord(BookList bookList) {
        User owner = userMapper.selectById(bookList.getUserId());
        List<Book> visibleBooks = bookListItemMapper.selectList(new LambdaQueryWrapper<BookListItem>()
                .eq(BookListItem::getBookListId, bookList.getId())
                .orderByAsc(BookListItem::getSortOrder)
                .orderByAsc(BookListItem::getId))
            .stream()
            .map(BookListItem::getBookId)
            .map(bookMapper::selectById)
            .filter(book -> book != null && !Integer.valueOf(1).equals(book.getIsDeleted()))
            .toList();
        List<Long> bookIds = visibleBooks.stream()
            .map(Book::getId)
            .toList();
        List<Map<String, Object>> bookInfos = visibleBooks.stream()
            .map(book -> {
                Map<String, Object> data = new HashMap<>();
                data.put("id", book.getId());
                data.put("bookId", book.getId());
                data.put("title", book.getTitle());
                data.put("bookTitle", book.getTitle());
                data.put("author", book.getAuthor());
                data.put("bookAuthor", book.getAuthor());
                data.put("description", book.getDescription());
                return data;
            })
            .toList();
        boolean publicVisible = isPublic(bookList);

        Map<String, Object> record = new HashMap<>();
        record.put("id", bookList.getId());
        record.put("listId", bookList.getId());
        record.put("owner", owner != null ? owner.getUsername() : null);
        record.put("username", owner != null ? owner.getUsername() : null);
        record.put("user", owner != null ? owner.getUsername() : null);
        record.put("userId", bookList.getUserId());
        record.put("title", bookList.getTitle());
        record.put("description", bookList.getDescription());
        record.put("intro", bookList.getDescription());
        record.put("bookIds", bookIds);
        record.put("bookIdList", bookIds);
        record.put("books", bookIds);
        record.put("bookInfos", bookInfos);
        record.put("bookCount", bookIds.size());
        record.put("count", bookIds.size());
        record.put("publicVisible", publicVisible);
        record.put("isPublic", publicVisible);
        record.put("public", publicVisible);
        record.put("visible", publicVisible);
        record.put("visibility", publicVisible ? "PUBLIC" : "PRIVATE");
        record.put("privacy", publicVisible ? "PUBLIC" : "PRIVATE");
        record.put("mode", publicVisible ? "PUBLIC" : "PRIVATE");
        record.put("createTime", bookList.getCreateTime());
        record.put("updatedAt", bookList.getUpdateTime());
        return record;
    }
}

