package com.example.library.service;

import com.example.library.dto.BookDto;
import com.example.library.dto.BookListItem;
import com.example.library.jooq.enums.BookStatus;
import com.example.library.jooq.tables.records.BookRecord;
import java.util.List;
import java.util.Locale;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.library.jooq.tables.Author.AUTHOR;
import static com.example.library.jooq.tables.Book.BOOK;
import static com.example.library.jooq.tables.Category.CATEGORY;

@Service
public class BookService {
    private final DSLContext dsl;

    public BookService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<BookListItem> search(String q, int page, int size, SortField<?> sortField) {
        Condition condition = DSL.trueCondition();
        if (q != null && !q.isBlank()) {
            String keyword = "%" + q.trim() + "%";
            condition = condition.and(
                    BOOK.TITLE.likeIgnoreCase(keyword)
                            .or(AUTHOR.NAME.likeIgnoreCase(keyword))
                            .or(CATEGORY.NAME.likeIgnoreCase(keyword))
            );
        }
        return fetchBooks(condition, sortField, size, page * size);
    }

    public List<BookListItem> listAvailable() {
        Condition condition = BOOK.STOCK.gt(0)
                .and(BOOK.STATUS.eq(BookStatus.AVAILABLE));
        return fetchBooks(condition, BOOK.TITLE.asc(), Integer.MAX_VALUE, 0);
    }

    private List<BookListItem> fetchBooks(Condition condition, SortField<?> sortField, int limit, int offset) {
        return baseSelect()
                .where(condition)
                .orderBy(sortField == null ? BOOK.TITLE.asc() : sortField)
                .limit(limit)
                .offset(offset)
                .fetch(this::mapToBookListItem);
    }

    private SelectJoinStep<?> baseSelect() {
        return dsl.select(
                        BOOK.BOOK_ID,
                        BOOK.TITLE,
                        BOOK.AUTHOR_ID,
                        AUTHOR.NAME,
                        BOOK.CATEGORY_ID,
                        CATEGORY.NAME,
                        BOOK.PRICE,
                        BOOK.STOCK,
                        BOOK.STATUS
                )
                .from(BOOK)
                .leftJoin(AUTHOR).on(BOOK.AUTHOR_ID.eq(AUTHOR.AUTHOR_ID))
                .leftJoin(CATEGORY).on(BOOK.CATEGORY_ID.eq(CATEGORY.CATEGORY_ID));
    }

    private BookListItem mapToBookListItem(Record record) {
        return new BookListItem(
                record.get(BOOK.BOOK_ID),
                record.get(BOOK.TITLE),
                record.get(BOOK.AUTHOR_ID),
                record.get(AUTHOR.NAME),
                record.get(BOOK.CATEGORY_ID),
                record.get(CATEGORY.NAME),
                record.get(BOOK.PRICE),
                record.get(BOOK.STOCK),
                record.get(BOOK.STATUS)
        );
    }

    public BookRecord getById(Long id) {
        return dsl.selectFrom(BOOK)
                .where(BOOK.BOOK_ID.eq(id))
                .fetchOne();
    }

    @Transactional
    public Long create(BookDto dto) {
        BookRecord record = dsl.newRecord(BOOK);
        record.setTitle(dto.title());
        record.setAuthorId(dto.authorId());
        record.setCategoryId(dto.categoryId());
        record.setPublisher(dto.publisher());
        record.setPublishedYear(dto.publishedYear());
        record.setIsbn(dto.isbn());
        record.setPrice(dto.price());
        record.setStock(dto.stock());
        record.setStatus(resolveStatus(dto.status()));
        record.store();
        return record.getBookId();
    }

    @Transactional
    public void update(Long id, BookDto dto) {
        int rowsUpdated = dsl.update(BOOK)
                .set(BOOK.TITLE, dto.title())
                .set(BOOK.AUTHOR_ID, dto.authorId())
                .set(BOOK.CATEGORY_ID, dto.categoryId())
                .set(BOOK.PUBLISHER, dto.publisher())
                .set(BOOK.PUBLISHED_YEAR, dto.publishedYear())
                .set(BOOK.ISBN, dto.isbn())
                .set(BOOK.PRICE, dto.price())
                .set(BOOK.STOCK, dto.stock())
                .set(BOOK.STATUS, resolveStatus(dto.status()))
                .where(BOOK.BOOK_ID.eq(id))
                .execute();
        if (rowsUpdated == 0) {
            throw new IllegalArgumentException("Book not found: " + id);
        }
    }

    @Transactional
    public void delete(Long id) {
        dsl.deleteFrom(BOOK)
                .where(BOOK.BOOK_ID.eq(id))
                .execute();
    }

    private BookStatus resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return BookStatus.AVAILABLE;
        }
        try {
            return BookStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid book status: " + status, ex);
        }
    }

    public long countAll() {
        return dsl.fetchCount(BOOK);
    }

    public long countAvailable() {
        return dsl.fetchCount(
                dsl.selectFrom(BOOK)
                        .where(BOOK.STOCK.gt(0)
                                .and(BOOK.STATUS.eq(BookStatus.AVAILABLE)))
        );
    }
}
