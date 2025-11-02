package com.example.library.controller.web;

import com.example.library.dto.BookDto;
import com.example.library.dto.BookListItem;
import com.example.library.jooq.enums.BookStatus;
import com.example.library.jooq.tables.records.AuthorRecord;
import com.example.library.jooq.tables.records.BookRecord;
import com.example.library.jooq.tables.records.CategoryRecord;
import com.example.library.service.AuthorService;
import com.example.library.service.BookService;
import com.example.library.service.CategoryService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        List<BookListItem> books = bookService.search(q, page, size, null);
        model.addAttribute("books", books);
        model.addAttribute("q", q);
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

    private void populateReferenceData(Model model) {
        model.addAttribute("authors", authorService.listAll());
        model.addAttribute("categories", categoryService.listAll());
    }
}
