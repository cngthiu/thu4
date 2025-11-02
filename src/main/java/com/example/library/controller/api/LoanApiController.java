//src/main/java/com/example/library/controller/api/LoanApiController.java
package com.example.library.controller.api;

import com.example.library.service.LoanService;
import org.jooq.Record;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanApiController {
    private final LoanService loanService;
    public LoanApiController(LoanService loanService) { this.loanService = loanService; }

    @PostMapping("/borrow")
    public Long borrow(@RequestParam Long bookId, @RequestParam Long memberId,
                       @RequestParam(defaultValue = "14") int days) {
        if (days < 1) throw new IllegalArgumentException("days must be >= 1");
        return loanService.borrow(bookId, memberId, days);
    }
    @PostMapping("/{loanId}/return") public void returnBook(@PathVariable Long loanId) { loanService.returnBook(loanId); }
    @GetMapping
    public List<Record> list(@RequestParam(required = false) String q,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) {
        return loanService.list(q, status, page, size);
    }
}
