package com.example.library.controller.web;

import com.example.library.jooq.tables.records.NotificationHistoryRecord;
import com.example.library.jooq.tables.records.NotificationRecord;
import com.example.library.service.NotificationService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NotificationsPageController {
    private final NotificationService notificationService;

    public NotificationsPageController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String list(Model model) {
        List<NotificationRecord> pending = notificationService.findPending();
        List<NotificationHistoryRecord> history = notificationService.findRecentHistory(50);
        model.addAttribute("pending", pending);
        model.addAttribute("history", history);
        model.addAttribute("pendingCount", notificationService.countPending());
        return "notifications/index";
    }
}
