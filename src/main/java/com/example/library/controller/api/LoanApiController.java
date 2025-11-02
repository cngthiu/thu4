package com.example.library.controller.api;

import com.example.library.dto.LoanBorrowRequest;
import com.example.library.dto.LoanListItem;
import com.example.library.service.LoanService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanApiController {
    private final LoanService loanService;

    public LoanApiController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/borrow")
    public List<Long> borrow(@Valid @RequestBody LoanBorrowRequest request) {
        return loanService.borrow(
                request.memberId(),
                request.bookIds(),
                request.borrowDate(),
                request.dueDate()
        );
    }

    @PostMapping("/{loanId}/return")
    public void returnBook(@PathVariable Long loanId,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate) {
        loanService.returnBook(loanId, returnDate);
    }

    @GetMapping
    public List<LoanListItem> list(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return loanService.list(q, status, page, size);
    }
}
