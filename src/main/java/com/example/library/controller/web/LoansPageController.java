//src/main/java/com/example/library/controller/web/LoansPageController.java
package com.example.library.controller.web;

import com.example.library.dto.BookListItem;
import com.example.library.dto.LoanBorrowForm;
import com.example.library.dto.LoanListItem;
import com.example.library.jooq.enums.LoanStatus;
import com.example.library.jooq.tables.records.MemberRecord;
import com.example.library.service.BookService;
import com.example.library.service.LoanService;
import com.example.library.service.MemberService;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
import org.springframework.format.annotation.DateTimeFormat;

@Controller
@RequestMapping("/loans")
public class LoansPageController {
    private final LoanService loanService;
    private final MemberService memberService;
    private final BookService bookService;

    public LoansPageController(LoanService loanService,
                               MemberService memberService,
                               BookService bookService) {
        this.loanService = loanService;
        this.memberService = memberService;
        this.bookService = bookService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        List<LoanListItem> loans = loanService.list(q, status, page, size);
        model.addAttribute("pageTitle", "Loans");
        model.addAttribute("pageId", "loans");
        model.addAttribute("q", q);
        model.addAttribute("loans", loans);
        model.addAttribute("status", status);
        model.addAttribute("size", size);
        model.addAttribute("statuses", LoanStatus.values());
        return "loans/index";
    }

    @GetMapping("/borrow")
    public String borrowForm(Model model) {
        LoanBorrowForm form = new LoanBorrowForm();
        form.setBorrowDate(LocalDate.now());
        form.setDueDate(LocalDate.now().plusDays(14));
        model.addAttribute("form", form);
        model.addAttribute("pageTitle", "Borrow books");
        model.addAttribute("pageId", "loans");
        populateBorrowReferences(model);
        return "loans/borrow";
    }

    @PostMapping("/borrow")
    public String borrow(@Valid @ModelAttribute("form") LoanBorrowForm form,
                         BindingResult br,
                         Model model) {
        if (br.hasErrors()) {
            model.addAttribute("pageTitle", "Borrow books");
            model.addAttribute("pageId", "loans");
            populateBorrowReferences(model);
            return "loans/borrow";
        }
        try {
            loanService.borrow(form.getMemberId(), form.getBookIds(), form.getBorrowDate(), form.getDueDate());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            br.reject("borrow.error", ex.getMessage());
            model.addAttribute("pageTitle", "Borrow books");
            model.addAttribute("pageId", "loans");
            populateBorrowReferences(model);
            return "loans/borrow";
        }
        return "redirect:/loans";
    }

    @PostMapping("/{loanId}/return")
    public String returnBook(@PathVariable Long loanId,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                             LocalDate returnDate) {
        loanService.returnBook(loanId, returnDate);
        return "redirect:/loans";
    }

    private void populateBorrowReferences(Model model) {
        List<MemberRecord> members = memberService.listActiveMembers();
        List<BookListItem> books = bookService.listAvailable();
        model.addAttribute("members", members);
        model.addAttribute("books", books);
    }
}
