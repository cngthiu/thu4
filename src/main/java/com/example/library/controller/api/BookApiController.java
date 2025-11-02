package com.example.library.controller.api;

import com.example.library.dto.BookDto;
import com.example.library.dto.BookListItem;
import com.example.library.service.BookService;
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

    public BookApiController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookListItem> list(@RequestParam(required = false) String q,
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
        return bookService.search(q, page, size, sortField);
    }

    @PostMapping
    public Long create(@Valid @RequestBody BookDto dto) {
        return bookService.create(dto);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @Valid @RequestBody BookDto dto) {
        bookService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        bookService.delete(id);
    }
}
