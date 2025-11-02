package com.example.library.service;

import com.example.library.dto.CategoryDto;
import com.example.library.jooq.tables.records.CategoryRecord;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.library.jooq.tables.Category.CATEGORY;

@Service
public class CategoryService {
    private final DSLContext dsl;

    public CategoryService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Result<CategoryRecord> search(String q, int page, int size) {
        Condition condition = DSL.trueCondition();
        if (q != null && !q.isBlank()) {
            condition = condition.and(CATEGORY.NAME.likeIgnoreCase("%" + q.trim() + "%"));
        }
        return dsl.selectFrom(CATEGORY)
                .where(condition)
                .orderBy(CATEGORY.NAME.asc())
                .limit(size)
                .offset(page * size)
                .fetch();
    }

    public List<CategoryRecord> listAll() {
        return dsl.selectFrom(CATEGORY)
                .orderBy(CATEGORY.NAME.asc())
                .fetch();
    }

    public CategoryRecord getById(Long id) {
        return dsl.selectFrom(CATEGORY)
                .where(CATEGORY.CATEGORY_ID.eq(id))
                .fetchOne();
    }

    @Transactional
    public Long create(CategoryDto dto) {
        CategoryRecord record = dsl.newRecord(CATEGORY);
        record.setName(dto.name());
        record.store();
        return record.getCategoryId();
    }

    @Transactional
    public void update(Long id, CategoryDto dto) {
        int rows = dsl.update(CATEGORY)
                .set(CATEGORY.NAME, dto.name())
                .where(CATEGORY.CATEGORY_ID.eq(id))
                .execute();
        if (rows == 0) {
            throw new IllegalArgumentException("Category not found: " + id);
        }
    }

    @Transactional
    public void delete(Long id) {
        dsl.deleteFrom(CATEGORY)
                .where(CATEGORY.CATEGORY_ID.eq(id))
                .execute();
    }
}
