//src/main/java/com/example/library/dto/BookDto.java
package com.example.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BookDto(
        Long id,
        @NotBlank String title,
        @NotNull Long authorId,
        @NotNull Long categoryId,
        String publisher,
        Integer publishedYear,
        String isbn,
        BigDecimal price,
        @NotNull @Min(0) Integer stock,
        String status
) {}
