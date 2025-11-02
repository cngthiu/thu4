package com.example.library.service;

import com.example.library.dto.BookDto;
import com.example.library.jooq.enums.BookStatus;
import com.example.library.jooq.tables.records.BookRecord;
import java.util.Locale;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.library.jooq.tables.Book.BOOK;

@Service
public class BookService {
    private final DSLContext dsl;

    public BookService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Result<BookRecord> search(String q, int page, int size, SortField<?> sortField) {
        Condition condition = DSL.trueCondition();
        if (q != null && !q.isBlank()) {
            condition = condition.and(BOOK.TITLE.likeIgnoreCase("%" + q.trim() + "%"));
        }
        return dsl.selectFrom(BOOK)
                .where(condition)
                .orderBy(sortField == null ? BOOK.TITLE.asc() : sortField)
                .limit(size)
                .offset(page * size)
                .fetch();
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
}
