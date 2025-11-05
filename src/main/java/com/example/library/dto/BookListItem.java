package com.example.library.dto;

import com.example.library.jooq.enums.BookStatus;
import java.math.BigDecimal;

public record BookListItem(
        Long bookId,
        String title,
        Long authorId,
        String authorName,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        Integer stock,
        BookStatus status,
        String coverPath
) {}
