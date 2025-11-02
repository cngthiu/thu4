//src/main/java/com/example/library/controller/web/BooksPageController.java
package com.example.library.controller.web;

import com.example.library.dto.BookDto;
import com.example.library.jooq.enums.BookStatus;
import com.example.library.jooq.tables.records.BookRecord;
import com.example.library.service.BookService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/books")
public class BooksPageController {
    private final BookService bookService;
    public BooksPageController(BookService bookService) { this.bookService = bookService; }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        List<BookRecord> books = bookService.search(q, page, size, null);
        model.addAttribute("books", books);
        model.addAttribute("q", q);
        return "books/index";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("book", new BookDto(null, "", 1L, 1L, null, null, null, BigDecimal.ZERO, 0, BookStatus.AVAILABLE.name()));
        return "books/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("book") BookDto dto, BindingResult br) {
        if (br.hasErrors()) return "books/form";
        bookService.create(dto);
        return "redirect:/books";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        BookRecord rec = bookService.getById(id);
        if (rec == null) return "redirect:/books";
        BookDto dto = new BookDto(
                rec.getBookId(), rec.getTitle(), rec.getAuthorId(), rec.getCategoryId(),
                rec.getPublisher(), rec.getPublishedYear(), rec.getIsbn(), rec.getPrice(),
                rec.getStock(), rec.getStatus() == null ? null : rec.getStatus().name()
        );
        model.addAttribute("book", dto);
        return "books/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("book") BookDto dto, BindingResult br) {
        if (br.hasErrors()) return "books/form";
        bookService.update(id, dto);
        return "redirect:/books";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        bookService.delete(id);
        return "redirect:/books";
    }
}
