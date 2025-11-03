package com.example.library.controller.web;

import com.example.library.dto.BookDto;
import com.example.library.dto.BookListItem;
import com.example.library.dto.PagedResult;
import com.example.library.jooq.enums.BookStatus;
import com.example.library.jooq.tables.records.AuthorRecord;
import com.example.library.jooq.tables.records.BookRecord;
import com.example.library.jooq.tables.records.CategoryRecord;
import com.example.library.service.AuthorService;
import com.example.library.service.BookService;
import com.example.library.service.CategoryService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.jooq.SortField;

import static com.example.library.jooq.tables.Book.BOOK;

@Controller
@RequestMapping("/books")
public class BooksPageController {
    private final BookService bookService;
    private final AuthorService authorService;
    private final CategoryService categoryService;

    public BooksPageController(BookService bookService,
                               AuthorService authorService,
                               CategoryService categoryService) {
        this.bookService = bookService;
        this.authorService = authorService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long authorId,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String minPrice,
                       @RequestParam(required = false) String maxPrice,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String sort,
                       Model model) {
        SortField<?> sortField = resolveSort(sort);
        BookStatus bookStatus = parseStatus(status);
        PagedResult<BookListItem> result = bookService.search(
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
        model.addAttribute("pageTitle", "Books");
        model.addAttribute("booksPage", result);
        model.addAttribute("books", result.items());
        model.addAttribute("q", q);
        model.addAttribute("authorId", authorId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("pageId", "books");
        model.addAttribute("authors", authorService.listAll());
        model.addAttribute("categories", categoryService.listAll());
        model.addAttribute("statuses", BookStatus.values());
        return "books/index";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        List<AuthorRecord> authors = authorService.listAll();
        List<CategoryRecord> categories = categoryService.listAll();
        Long defaultAuthor = authors.isEmpty() ? null : authors.get(0).getAuthorId();
        Long defaultCategory = categories.isEmpty() ? null : categories.get(0).getCategoryId();
        model.addAttribute("book", new BookDto(null, "", defaultAuthor, defaultCategory, null, null, null, BigDecimal.ZERO, 0, BookStatus.AVAILABLE.name()));
        model.addAttribute("authors", authors);
        model.addAttribute("categories", categories);
        return "books/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("book") BookDto dto, BindingResult br, Model model) {
        if (br.hasErrors()) {
            populateReferenceData(model);
            return "books/form";
        }
        bookService.create(dto);
        return "redirect:/books";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        BookRecord rec = bookService.getById(id);
        if (rec == null) {
            return "redirect:/books";
        }
        BookDto dto = new BookDto(
                rec.getBookId(),
                rec.getTitle(),
                rec.getAuthorId(),
                rec.getCategoryId(),
                rec.getPublisher(),
                rec.getPublishedYear(),
                rec.getIsbn(),
                rec.getPrice(),
                rec.getStock(),
                rec.getStatus() == null ? null : rec.getStatus().name()
        );
        model.addAttribute("book", dto);
        populateReferenceData(model);
        return "books/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("book") BookDto dto, BindingResult br, Model model) {
        if (br.hasErrors()) {
            populateReferenceData(model);
            return "books/form";
        }
        bookService.update(id, dto);
        return "redirect:/books";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        bookService.delete(id);
        return "redirect:/books";
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> export(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) Long authorId,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String minPrice,
                                                    @RequestParam(required = false) String maxPrice,
                                                    @RequestParam(required = false) String sort) {
        SortField<?> sortField = resolveSort(sort);
        List<BookListItem> rows = bookService.searchAll(
                q,
                authorId,
                categoryId,
                parseStatus(status),
                parseDecimal(minPrice),
                parseDecimal(maxPrice),
                sortField
        );
        StringBuilder csv = new StringBuilder("ID,Title,Author,Category,Price,Stock,Status\n");
        rows.forEach(row -> csv.append(csvEscape(row.bookId()))
                .append(',')
                .append(csvEscape(row.title()))
                .append(',')
                .append(csvEscape(row.authorName()))
                .append(',')
                .append(csvEscape(row.categoryName()))
                .append(',')
                .append(csvEscape(row.price()))
                .append(',')
                .append(csvEscape(row.stock()))
                .append(',')
                .append(csvEscape(row.status()))
                .append('\n'));

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=books.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(bytes.length)
                .body(resource);
    }

    private void populateReferenceData(Model model) {
        model.addAttribute("authors", authorService.listAll());
        model.addAttribute("categories", categoryService.listAll());
    }

    private SortField<?> resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return BOOK.TITLE.asc();
        }
        String[] parts = sort.split(",");
        String field = parts[0];
        boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1]);
        if ("price".equalsIgnoreCase(field)) {
            return desc ? BOOK.PRICE.desc() : BOOK.PRICE.asc();
        }
        if ("stock".equalsIgnoreCase(field)) {
            return desc ? BOOK.STOCK.desc() : BOOK.STOCK.asc();
        }
        return desc ? BOOK.TITLE.desc() : BOOK.TITLE.asc();
    }

    private BookStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return BookStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String csvEscape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains("\"") || text.contains(",") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
