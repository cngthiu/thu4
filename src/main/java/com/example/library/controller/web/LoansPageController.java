package com.example.library.controller.web;

import com.example.library.jooq.tables.records.LoanRecord;
import com.example.library.service.LoanService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/loans")
public class LoansPageController {
    private final LoanService loanService;

    public LoansPageController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        List<LoanRecord> loans = loanService.list(null, status, page, size);
        model.addAttribute("loans", loans);
        model.addAttribute("status", status);
        return "loans/index";
    }

    @GetMapping("/borrow")
    public String borrowForm() {
        return "loans/borrow";
    }

    @PostMapping("/borrow")
    public String borrow(@RequestParam Long bookId,
                         @RequestParam Long memberId,
                         @RequestParam(defaultValue = "14") int days) {
        loanService.borrow(bookId, memberId, days);
        return "redirect:/loans";
    }

    @PostMapping("/{loanId}/return")
    public String returnBook(@PathVariable Long loanId) {
        loanService.returnBook(loanId);
        return "redirect:/loans";
    }
}
