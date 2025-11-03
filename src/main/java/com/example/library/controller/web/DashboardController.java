package com.example.library.controller.web;

import com.example.library.dto.BorrowReturnStat;
import com.example.library.dto.ChartDataPoint;
import com.example.library.service.BookService;
import com.example.library.service.LoanService;
import com.example.library.service.MemberService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final LoanService loanService;
    private final BookService bookService;
    private final MemberService memberService;

    public DashboardController(LoanService loanService,
                               BookService bookService,
                               MemberService memberService) {
        this.loanService = loanService;
        this.bookService = bookService;
        this.memberService = memberService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("totalBooks", bookService.countAll());
        model.addAttribute("availableBooks", bookService.countAvailable());
        model.addAttribute("totalMembers", memberService.countAll());
        model.addAttribute("activeMembers", memberService.countActive());
        model.addAttribute("borrowedToday", loanService.countBorrowedToday());
        model.addAttribute("overdueLoans", loanService.countOverdue());
        model.addAttribute("recentLoans", loanService.findRecentLoans(5));
        model.addAttribute("totalFineAmount", loanService.sumFineAmount());
        model.addAttribute("borrowReturnData", toBorrowReturnSeries(loanService.borrowReturnStats(30)));
        model.addAttribute("topBooksData", toChartSeries(loanService.topBorrowedBooks(10)));
        model.addAttribute("categoryBorrowData", toChartSeries(loanService.borrowByCategory()));
        model.addAttribute("topMembersData", toChartSeries(loanService.topMembers(10)));
        model.addAttribute("overdueTimelineData", toChartSeries(loanService.overdueTimeline(12)));
        return "dashboard/index";
    }

    private List<Map<String, Object>> toBorrowReturnSeries(List<BorrowReturnStat> stats) {
        return stats.stream()
                .map(s -> Map.<String, Object>of(
                        "period", s.period(),
                        "borrowed", s.borrowed(),
                        "returned", s.returned()))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> toChartSeries(List<ChartDataPoint> points) {
        return points.stream()
                .map(p -> Map.<String, Object>of(
                        "label", p.label(),
                        "value", p.value()))
                .collect(Collectors.toList());
    }
}
