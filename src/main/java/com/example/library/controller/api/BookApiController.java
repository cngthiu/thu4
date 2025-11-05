package com.example.library.controller.api;

import com.example.library.dto.BookDto;
import com.example.library.dto.BookListItem;
import com.example.library.dto.PagedResult;
import com.example.library.jooq.enums.BookStatus;
import com.example.library.service.BookService;
import com.example.library.service.CoverStorageService;
import jakarta.validation.Valid;
import java.util.List;
import org.jooq.SortField;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.example.library.jooq.tables.Book.BOOK;

@RestController
@RequestMapping("/api/books")
public class BookApiController {
    private final BookService bookService;
    private final CoverStorageService coverStorageService;

    public BookApiController(BookService bookService, CoverStorageService coverStorageService) {
        this.bookService = bookService;
        this.coverStorageService = coverStorageService;
    }

    @GetMapping
    public PagedResult<BookListItem> list(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) Long authorId,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String minPrice,
                                          @RequestParam(required = false) String maxPrice,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false, name = "sort") String sort) {
        SortField<?> sortField = null;
        if (sort != null) {
            boolean desc = sort.endsWith(",desc");
            String col = sort.split(",")[0];
            if ("price".equalsIgnoreCase(col)) {
                sortField = desc ? BOOK.PRICE.desc() : BOOK.PRICE.asc();
            } else if ("title".equalsIgnoreCase(col)) {
                sortField = desc ? BOOK.TITLE.desc() : BOOK.TITLE.asc();
            }
        }
        BookStatus bookStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                bookStatus = BookStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                bookStatus = null;
            }
        }
        return bookService.search(
                q,
                authorId,
                categoryId,
                bookStatus,
                parseDecimal(minPrice),
                parseDecimal(maxPrice),
                page,
                size,
                sortField
        );
    }

    @PostMapping
    public Long create(@Valid @RequestBody BookDto dto) {
        return bookService.create(dto, dto.coverPath());
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @Valid @RequestBody BookDto dto) {
        bookService.update(id, dto, dto.coverPath());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        String coverPath = bookService.delete(id);
        coverStorageService.delete(coverPath);
    }

    private java.math.BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new java.math.BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
