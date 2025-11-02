//src/main/java/com/example/library/service/BookService.java
package com.example.library.service;

import com.example.library.dto.BookDto;
import com.example.library.jooq.tables.records.BookRecord;
import org.jooq.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.library.jooq.tables.Book.BOOK;

@Service
public class BookService {
    private final DSLContext dsl;
    public BookService(DSLContext dsl) { this.dsl = dsl; }

    public Result<Record> search(String q, int page, int size, SortField<?> sortField) {
        Condition cond = DSL.trueCondition();
        if (q != null && !q.isBlank()) cond = cond.and(BOOK.TITLE.likeIgnoreCase("%" + q.trim() + "%"));
        return dsl.select().from(BOOK).where(cond)
                .orderBy(sortField == null ? BOOK.TITLE.asc() : sortField)
                .limit(size).offset(page * size).fetch();
    }

    public BookRecord getById(Long id) {
        return dsl.selectFrom(BOOK).where(BOOK.BOOK_ID.eq(id)).fetchOne();
    }

    @Transactional
    public Long create(BookDto dto) {
        BookRecord r = dsl.newRecord(BOOK);
        r.setTitle(dto.title());
        r.setAuthorId(dto.authorId());
        r.setCategoryId(dto.categoryId());
        r.setPublisher(dto.publisher());
        r.setPublishedYear(dto.publishedYear());
        r.setIsbn(dto.isbn());
        r.setPrice(dto.price());
        r.setStock(dto.stock());
        r.setStatus(dto.status() == null ? "AVAILABLE" : dto.status());
        r.store();
        return r.getBookId();
    }

    @Transactional
    public void update(Long id, BookDto dto) {
        int rows = dsl.update(BOOK)
                .set(BOOK.TITLE, dto.title())
                .set(BOOK.AUTHOR_ID, dto.authorId())
                .set(BOOK.CATEGORY_ID, dto.categoryId())
                .set(BOOK.PUBLISHER, dto.publisher())
                .set(BOOK.PUBLISHED_YEAR, dto.publishedYear())
                .set(BOOK.ISBN, dto.isbn())
                .set(BOOK.PRICE, dto.price())
                .set(BOOK.STOCK, dto.stock())
                .set(BOOK.STATUS, dto.status() == null ? "AVAILABLE" : dto.status())
                .where(BOOK.BOOK_ID.eq(id)).execute();
        if (rows == 0) throw new IllegalArgumentException("Book not found: " + id);
    }

    @Transactional
    public void delete(Long id) {
        dsl.deleteFrom(BOOK).where(BOOK.BOOK_ID.eq(id)).execute();
    }
}
