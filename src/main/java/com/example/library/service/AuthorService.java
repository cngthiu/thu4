package com.example.library.service;

import com.example.library.dto.AuthorDto;
import com.example.library.jooq.tables.records.AuthorRecord;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.library.jooq.tables.Author.AUTHOR;

@Service
public class AuthorService {
    private final DSLContext dsl;

    public AuthorService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Result<AuthorRecord> search(String q, int page, int size) {
        Condition condition = DSL.trueCondition();
        if (q != null && !q.isBlank()) {
            String keyword = "%" + q.trim() + "%";
            condition = condition.and(
                    AUTHOR.NAME.likeIgnoreCase(keyword)
                            .or(AUTHOR.NATIONALITY.likeIgnoreCase(keyword))
            );
        }
        return dsl.selectFrom(AUTHOR)
                .where(condition)
                .orderBy(AUTHOR.NAME.asc())
                .limit(size)
                .offset(page * size)
                .fetch();
    }

    public List<AuthorRecord> listAll() {
        return dsl.selectFrom(AUTHOR)
                .orderBy(AUTHOR.NAME.asc())
                .fetch();
    }

    public AuthorRecord getById(Long id) {
        return dsl.selectFrom(AUTHOR)
                .where(AUTHOR.AUTHOR_ID.eq(id))
                .fetchOne();
    }

    @Transactional
    public Long create(AuthorDto dto) {
        AuthorRecord record = dsl.newRecord(AUTHOR);
        record.setName(dto.name());
        record.setNationality(dto.nationality());
        record.store();
        return record.getAuthorId();
    }

    @Transactional
    public void update(Long id, AuthorDto dto) {
        int rows = dsl.update(AUTHOR)
                .set(AUTHOR.NAME, dto.name())
                .set(AUTHOR.NATIONALITY, dto.nationality())
                .where(AUTHOR.AUTHOR_ID.eq(id))
                .execute();
        if (rows == 0) {
            throw new IllegalArgumentException("Author not found: " + id);
        }
    }

    @Transactional
    public void delete(Long id) {
        dsl.deleteFrom(AUTHOR)
                .where(AUTHOR.AUTHOR_ID.eq(id))
                .execute();
    }
}
