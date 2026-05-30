package com.tihu.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tihu.backend.common.ApiException;
import com.tihu.backend.entity.Book;
import com.tihu.backend.entity.BookTag;
import com.tihu.backend.entity.Collection;
import com.tihu.backend.entity.Tag;
import com.tihu.backend.mapper.BookMapper;
import com.tihu.backend.mapper.BookTagMapper;
import com.tihu.backend.mapper.CollectionMapper;
import com.tihu.backend.service.BookService;
import com.tihu.backend.service.RatingService;
import com.tihu.backend.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {
    
    @Autowired
    private RatingService ratingService;

    @Autowired
    private TagService tagService;

    @Autowired
    private BookTagMapper bookTagMapper;

    @Autowired
    private CollectionMapper collectionMapper;


    @Override
    public Page<Book> getBooks(int pageNum, int pageSize, String sort) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>()
                .eq(Book::getIsDeleted, 0);
        applySort(wrapper, sort);
        Page<Book> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        attachTags(page.getRecords());
        return page;
    }

    @Override
    public Page<Book> searchByTitle(String keyword, int pageNum, int pageSize, String sort) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>()
                .eq(Book::getIsDeleted, 0)
                .like(Book::getTitle, keyword);
        applySort(wrapper, sort);
        Page<Book> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        attachTags(page.getRecords());
        return page;
    }

    @Override
    public Page<Book> searchByTags(List<String> tags, int pageNum, int pageSize, String sort) {
        int safePageSize = pageSize <= 0 ? 10 : pageSize;
        List<String> normalizedTags = normalizeTags(tags);
        Page<Book> page = new Page<>(pageNum, safePageSize);
        if (normalizedTags.isEmpty()) {
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            page.setCurrent(pageNum);
            page.setSize(safePageSize);
            return page;
        }

        List<Tag> tagList = tagService.list(new LambdaQueryWrapper<Tag>().in(Tag::getName, normalizedTags));
        if (tagList.size() != normalizedTags.size()) {
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            page.setCurrent(pageNum);
            page.setSize(safePageSize);
            return page;
        }

        List<Long> tagIds = tagList.stream().map(Tag::getId).collect(Collectors.toList());
        List<BookTag> relations = bookTagMapper.selectList(
                new LambdaQueryWrapper<BookTag>().in(BookTag::getTagId, tagIds)
        );
        if (relations.isEmpty()) {
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            page.setCurrent(pageNum);
            page.setSize(safePageSize);
            return page;
        }

        Map<Long, Set<Long>> bookTagMap = new java.util.HashMap<>();
        for (BookTag relation : relations) {
            bookTagMap.computeIfAbsent(relation.getBookId(), k -> new LinkedHashSet<>())
                    .add(relation.getTagId());
        }

        List<Long> matchedBookIds = bookTagMap.entrySet().stream()
                .filter(entry -> entry.getValue().containsAll(tagIds))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (matchedBookIds.isEmpty()) {
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            page.setCurrent(pageNum);
            page.setSize(safePageSize);
            return page;
        }

        List<Book> books = this.list(new LambdaQueryWrapper<Book>()
                .eq(Book::getIsDeleted, 0)
                .in(Book::getId, matchedBookIds));

        applySort(books, sort);

        attachTags(books);

        int total = books.size();
        int fromIndex = Math.max(0, (pageNum - 1) * safePageSize);
        int toIndex = Math.min(fromIndex + safePageSize, total);
        List<Book> records = fromIndex >= total ? Collections.emptyList() : books.subList(fromIndex, toIndex);

        page.setRecords(records);
        page.setTotal(total);
        page.setCurrent(pageNum);
        page.setSize(safePageSize);
        return page;
    }

    @Override
    public List<Tag> getBookTags(Long bookId) {
        Book book = this.getById(bookId);
        if (book == null || book.getIsDeleted() == 1) {
            throw new ApiException(404, "图书不存在");
        }

        List<BookTag> relations = bookTagMapper.selectList(
                new LambdaQueryWrapper<BookTag>().eq(BookTag::getBookId, bookId)
        );
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> tagIds = relations.stream().map(BookTag::getTagId).collect(Collectors.toList());
        return tagService.listByIds(tagIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceBookTags(Long bookId, List<String> tagNames) {
        Book book = this.getById(bookId);
        if (book == null || book.getIsDeleted() == 1) {
            throw new ApiException(404, "图书不存在");
        }

        bookTagMapper.delete(new LambdaQueryWrapper<BookTag>().eq(BookTag::getBookId, bookId));

        List<String> normalizedTags = normalizeTags(tagNames);
        if (normalizedTags.isEmpty()) {
            return;
        }

        for (String tagName : normalizedTags) {
            Tag tag = tagService.lambdaQuery().eq(Tag::getName, tagName).one();
            if (tag == null) {
                tag = new Tag();
                tag.setName(tagName);
                tagService.save(tag);
            }

            BookTag relation = new BookTag();
            relation.setBookId(bookId);
            relation.setTagId(tag.getId());
            bookTagMapper.insert(relation);
        }
    }

    @Override
    public Object getBookDetail(Long bookId) {
        Book book = this.getById(bookId);
        if (book == null || book.getIsDeleted() == 1) {
            return null;
        }
        attachTags(Collections.singletonList(book));

        // 返回包含评分统计的详情对象
        Object ratingStats = ratingService.getRatingStats(bookId);
        Long favoriteCount = collectionMapper.selectCount(
                new LambdaQueryWrapper<Collection>().eq(Collection::getBookId, bookId)
        );

        Map<String, Object> detail = new HashMap<>();
        detail.put("bookInfo", book);
        detail.put("ratings", ratingStats);
        detail.put("tags", book.getTags());
        detail.put("favoriteCount", favoriteCount);
        detail.put("favoritesCount", favoriteCount);
        detail.put("collectCount", favoriteCount);
        detail.put("collectionCount", favoriteCount);
        detail.put("collectedCount", favoriteCount);
        return detail;
    }

    private void attachTags(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return;
        }

        List<Long> bookIds = books.stream()
                .map(Book::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (bookIds.isEmpty()) {
            for (Book book : books) {
                book.setTags(Collections.emptyList());
            }
            return;
        }

        List<BookTag> relations = bookTagMapper.selectList(
                new LambdaQueryWrapper<BookTag>().in(BookTag::getBookId, bookIds)
        );
        if (relations.isEmpty()) {
            for (Book book : books) {
                book.setTags(Collections.emptyList());
            }
            return;
        }

        List<Long> tagIds = relations.stream()
                .map(BookTag::getTagId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> tagNameMap = tagService.listByIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));

        Map<Long, List<String>> bookTagMap = new HashMap<>();
        for (BookTag relation : relations) {
            String tagName = tagNameMap.get(relation.getTagId());
            if (tagName == null) {
                continue;
            }
            bookTagMap.computeIfAbsent(relation.getBookId(), k -> new ArrayList<>()).add(tagName);
        }

        for (Book book : books) {
            book.setTags(bookTagMap.getOrDefault(book.getId(), Collections.emptyList()));
        }
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String[] parts = tag.split("[,，]");
            for (String part : parts) {
                String value = part.trim();
                if (!value.isEmpty()) {
                    normalized.add(value);
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private void applySort(LambdaQueryWrapper<Book> wrapper, String sort) {
        String normalizedSort = normalizeSort(sort);
        if ("rating_desc".equals(normalizedSort)) {
            wrapper.orderByDesc(Book::getAvgRating).orderByAsc(Book::getTitle).orderByAsc(Book::getId);
        } else if ("title_asc".equals(normalizedSort)) {
            wrapper.orderByAsc(Book::getTitle).orderByAsc(Book::getId);
        } else {
            wrapper.orderByAsc(Book::getCreateTime).orderByAsc(Book::getId);
        }
    }

    private void applySort(List<Book> books, String sort) {
        if (books == null || books.size() <= 1) {
            return;
        }
        String normalizedSort = normalizeSort(sort);
        if ("rating_desc".equals(normalizedSort)) {
            books.sort((a, b) -> {
                int cmp = compareDoubleDesc(a == null ? null : a.getAvgRating(), b == null ? null : b.getAvgRating());
                if (cmp != 0) {
                    return cmp;
                }
                cmp = compareStringAsc(a == null ? null : a.getTitle(), b == null ? null : b.getTitle());
                if (cmp != 0) {
                    return cmp;
                }
                return compareLongAsc(a == null ? null : a.getId(), b == null ? null : b.getId());
            });
            return;
        }
        if ("title_asc".equals(normalizedSort)) {
            books.sort((a, b) -> {
                int cmp = compareStringAsc(a == null ? null : a.getTitle(), b == null ? null : b.getTitle());
                if (cmp != 0) {
                    return cmp;
                }
                return compareLongAsc(a == null ? null : a.getId(), b == null ? null : b.getId());
            });
            return;
        }
        books.sort((a, b) -> {
            int cmp = compareLocalDateTimeAsc(a == null ? null : a.getCreateTime(), b == null ? null : b.getCreateTime());
            if (cmp != 0) {
                return cmp;
            }
            return compareLongAsc(a == null ? null : a.getId(), b == null ? null : b.getId());
        });
    }

    private String normalizeSort(String sort) {
        if (sort == null) {
            return "default";
        }
        String value = sort.trim().toLowerCase();
        if (value.isEmpty() || "default".equals(value)) {
            return "default";
        }
        if ("rating_desc".equals(value) || "title_asc".equals(value)) {
            return value;
        }
        return "default";
    }

    private int compareDoubleDesc(Double left, Double right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return Double.compare(right, left);
    }

    private int compareStringAsc(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareToIgnoreCase(right);
    }

    private int compareLongAsc(Long left, Long right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return Long.compare(left, right);
    }

    private int compareLocalDateTimeAsc(java.time.LocalDateTime left, java.time.LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }
}

