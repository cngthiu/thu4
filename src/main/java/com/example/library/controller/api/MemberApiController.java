//src/main/java/com/example/library/controller/api/MemberApiController.java
package com.example.library.controller.api;

import com.example.library.dto.MemberDto;
import com.example.library.service.MemberService;
import jakarta.validation.Valid;
import org.jooq.Record;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberApiController {
    private final MemberService memberService;
    public MemberApiController(MemberService memberService) { this.memberService = memberService; }

    @GetMapping
    public List<Record> list(@RequestParam(required = false) String q,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) {
        return memberService.search(q, page, size);
    }
    @PostMapping public Long create(@Valid @RequestBody MemberDto dto) { return memberService.create(dto); }
    @PutMapping("/{id}") public void update(@PathVariable Long id, @Valid @RequestBody MemberDto dto) { memberService.update(id, dto); }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { memberService.delete(id); }
}
