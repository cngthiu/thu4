package com.example.library.controller.web;

import com.example.library.dto.AuthorDto;
import com.example.library.jooq.tables.records.AuthorRecord;
import com.example.library.service.AuthorService;
import jakarta.validation.Valid;
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
@RequestMapping("/authors")
public class AuthorsPageController {
    private final AuthorService authorService;

    public AuthorsPageController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        List<AuthorRecord> authors = authorService.search(q, page, size);
        model.addAttribute("authors", authors);
        model.addAttribute("q", q);
        return "authors/index";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("author", new AuthorDto(null, "", ""));
        return "authors/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("author") AuthorDto dto, BindingResult br) {
        if (br.hasErrors()) {
            return "authors/form";
        }
        authorService.create(dto);
        return "redirect:/authors";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AuthorRecord record = authorService.getById(id);
        if (record == null) {
            return "redirect:/authors";
        }
        model.addAttribute("author", new AuthorDto(
                record.getAuthorId(),
                record.getName(),
                record.getNationality()
        ));
        return "authors/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("author") AuthorDto dto, BindingResult br) {
        if (br.hasErrors()) {
            return "authors/form";
        }
        authorService.update(id, dto);
        return "redirect:/authors";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        authorService.delete(id);
        return "redirect:/authors";
    }
}
