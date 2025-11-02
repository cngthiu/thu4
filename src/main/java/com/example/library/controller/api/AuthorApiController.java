package com.example.library.controller.api;

import com.example.library.dto.AuthorDto;
import com.example.library.jooq.tables.records.AuthorRecord;
import com.example.library.service.AuthorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/authors")
public class AuthorApiController {
    private final AuthorService authorService;

    public AuthorApiController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    public List<AuthorRecord> list(@RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return authorService.search(q, page, size);
    }

    @PostMapping
    public Long create(@Valid @RequestBody AuthorDto dto) {
        return authorService.create(dto);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @Valid @RequestBody AuthorDto dto) {
        authorService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        authorService.delete(id);
    }
}
