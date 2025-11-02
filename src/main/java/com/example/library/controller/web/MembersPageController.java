//src/main/java/com/example/library/controller/web/MembersPageController.java
package com.example.library.controller.web;

import com.example.library.dto.MemberDto;
import com.example.library.jooq.enums.MemberStatus;
import com.example.library.jooq.tables.records.MemberRecord;
import com.example.library.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/members")
public class MembersPageController {
    private final MemberService memberService;
    public MembersPageController(MemberService memberService) { this.memberService = memberService; }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        List<MemberRecord> members = memberService.search(q, page, size);
        model.addAttribute("members", members);
        model.addAttribute("q", q);
        return "members/index";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("member", new MemberDto(null, "", "", "", MemberStatus.ACTIVE.name()));
        return "members/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("member") MemberDto dto, BindingResult br) {
        if (br.hasErrors()) return "members/form";
        memberService.create(dto);
        return "redirect:/members";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        MemberRecord rec = memberService.getById(id);
        if (rec == null) return "redirect:/members";
        MemberDto dto = new MemberDto(rec.getMemberId(), rec.getFullName(), rec.getEmail(), rec.getPhone(),
                rec.getStatus() == null ? null : rec.getStatus().name());
        model.addAttribute("member", dto);
        return "members/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("member") MemberDto dto, BindingResult br) {
        if (br.hasErrors()) return "members/form";
        memberService.update(id, dto);
        return "redirect:/members";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        memberService.delete(id);
        return "redirect:/members";
    }
}
