//src/main/java/com/example/library/scheduler/EmailNotificationScheduler.java
package com.example.library.scheduler;

import com.example.library.service.NotificationService;
import org.jooq.Record;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class EmailNotificationScheduler {
    private final NotificationService notificationService;
    public EmailNotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendPendingNotifications() {
        String pid = NotificationService.newProcessId();
        List<Record> batch = notificationService.claimBatch(pid, 300, 50);
        for (Record r : batch) {
            try {
                notificationService.sendEmail(r);
                notificationService.markSuccessAndArchive(r);
            } catch (Exception e) {
                notificationService.markFailureAndMaybeArchive(r, e.getMessage());
            }
        }
    }
}
