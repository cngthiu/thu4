package com.example.library.controller.web;

import com.example.library.dto.CategoryDto;
import com.example.library.jooq.tables.records.CategoryRecord;
import com.example.library.service.CategoryService;
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
@RequestMapping("/categories")
public class CategoriesPageController {
    private final CategoryService categoryService;

    public CategoriesPageController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        List<CategoryRecord> categories = categoryService.search(q, page, size);
        model.addAttribute("categories", categories);
        model.addAttribute("q", q);
        return "categories/index";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("category", new CategoryDto(null, ""));
        return "categories/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("category") CategoryDto dto, BindingResult br) {
        if (br.hasErrors()) {
            return "categories/form";
        }
        categoryService.create(dto);
        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        CategoryRecord record = categoryService.getById(id);
        if (record == null) {
            return "redirect:/categories";
        }
        model.addAttribute("category", new CategoryDto(
                record.getCategoryId(),
                record.getName()
        ));
        return "categories/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("category") CategoryDto dto, BindingResult br) {
        if (br.hasErrors()) {
            return "categories/form";
        }
        categoryService.update(id, dto);
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        categoryService.delete(id);
        return "redirect:/categories";
    }
}
