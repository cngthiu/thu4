//src/main/java/com/example/library/service/NotificationService.java
package com.example.library.service;

import com.example.library.config.MailConfig;
import com.example.library.jooq.tables.records.NotificationHistoryRecord;
import com.example.library.jooq.tables.records.NotificationRecord;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static com.example.library.jooq.tables.Notification.NOTIFICATION;
import static com.example.library.jooq.tables.NotificationHistory.NOTIFICATION_HISTORY;

@Service
public class NotificationService {
    private static final byte SUCCESS_FLAG = 1;
    private static final byte FAILURE_FLAG = 0;

    private final DSLContext dsl;
    private final JavaMailSender mailSender;
    private final MailConfig mailConfig;

    public NotificationService(DSLContext dsl, JavaMailSender mailSender, MailConfig mailConfig) {
        this.dsl = dsl; this.mailSender = mailSender; this.mailConfig = mailConfig;
    }

    @Transactional
    public List<Record> claimBatch(String processId, int timeoutSec, int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expired = now.minusSeconds(timeoutSec);
        dsl.update(NOTIFICATION)
           .set(NOTIFICATION.PROCESS_ID, processId)
           .set(NOTIFICATION.LOCKED_AT, now)
           .set(NOTIFICATION.RETRY_COUNT, NOTIFICATION.RETRY_COUNT.plus(1))
           .where(NOTIFICATION.LOCKED_AT.isNotNull().and(NOTIFICATION.LOCKED_AT.lt(expired))
                  .and(NOTIFICATION.RETRY_COUNT.lt(3))).execute();
        dsl.update(NOTIFICATION)
           .set(NOTIFICATION.PROCESS_ID, processId)
           .set(NOTIFICATION.LOCKED_AT, now)
           .set(NOTIFICATION.RETRY_COUNT, NOTIFICATION.RETRY_COUNT.plus(1))
           .where(NOTIFICATION.LOCKED_AT.isNull().and(NOTIFICATION.RETRY_COUNT.lt(3)))
           .limit(limit).execute();
        return dsl.select().from(NOTIFICATION).where(NOTIFICATION.PROCESS_ID.eq(processId))
                .orderBy(NOTIFICATION.CREATED_AT.asc()).limit(limit).fetch();
    }

    @Transactional
    public void markSuccessAndArchive(Record n) {
        dsl.insertInto(NOTIFICATION_HISTORY)
           .set(NOTIFICATION_HISTORY.MEMBER_ID, n.get(NOTIFICATION.MEMBER_ID))
           .set(NOTIFICATION_HISTORY.EMAIL, n.get(NOTIFICATION.EMAIL))
           .set(NOTIFICATION_HISTORY.SUBJECT, n.get(NOTIFICATION.SUBJECT))
           .set(NOTIFICATION_HISTORY.BODY, n.get(NOTIFICATION.BODY))
           .set(NOTIFICATION_HISTORY.SUCCESS, SUCCESS_FLAG)
           .set(NOTIFICATION_HISTORY.ERROR_MESSAGE, (String) null)
           .set(NOTIFICATION_HISTORY.CREATED_AT, n.get(NOTIFICATION.CREATED_AT))
           .execute();
        dsl.deleteFrom(NOTIFICATION).where(NOTIFICATION.ID.eq(n.get(NOTIFICATION.ID))).execute();
    }

    @Transactional
    public void markFailureAndMaybeArchive(Record n, String error) {
        int retry = n.get(NOTIFICATION.RETRY_COUNT);
        dsl.update(NOTIFICATION)
           .set(NOTIFICATION.LAST_ERROR, error)
           .set(NOTIFICATION.LAST_ATTEMPT_AT, LocalDateTime.now())
           .set(NOTIFICATION.PROCESS_ID, (String) null)
           .set(NOTIFICATION.LOCKED_AT, (LocalDateTime) null)
           .where(NOTIFICATION.ID.eq(n.get(NOTIFICATION.ID))).execute();
        if (retry >= 3) {
            dsl.insertInto(NOTIFICATION_HISTORY)
               .set(NOTIFICATION_HISTORY.MEMBER_ID, n.get(NOTIFICATION.MEMBER_ID))
               .set(NOTIFICATION_HISTORY.EMAIL, n.get(NOTIFICATION.EMAIL))
               .set(NOTIFICATION_HISTORY.SUBJECT, n.get(NOTIFICATION.SUBJECT))
               .set(NOTIFICATION_HISTORY.BODY, n.get(NOTIFICATION.BODY))
               .set(NOTIFICATION_HISTORY.SUCCESS, FAILURE_FLAG)
               .set(NOTIFICATION_HISTORY.ERROR_MESSAGE, error)
               .set(NOTIFICATION_HISTORY.CREATED_AT, n.get(NOTIFICATION.CREATED_AT))
               .execute();
            dsl.deleteFrom(NOTIFICATION).where(NOTIFICATION.ID.eq(n.get(NOTIFICATION.ID))).execute();
        }
    }

    public void sendEmail(Record n) {
        if (!mailConfig.isMailEnabled()) {
            System.out.println("[WARN] SMTP not configured; skip sending email to " + n.get(NOTIFICATION.EMAIL));
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(n.get(NOTIFICATION.EMAIL));
        msg.setSubject(n.get(NOTIFICATION.SUBJECT));
        msg.setText(n.get(NOTIFICATION.BODY));
        try { mailSender.send(msg); } catch (MailException e) { throw e; }
    }

    public static String newProcessId() { return UUID.randomUUID().toString(); }

    @Transactional
    public boolean queueNotification(Long memberId, String email, String subject, String body) {
        if (email == null || email.isBlank() || subject == null || body == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recent = now.minusHours(12);
        boolean exists = dsl.fetchExists(
                dsl.selectOne().from(NOTIFICATION)
                        .where(NOTIFICATION.MEMBER_ID.eq(memberId)
                                .and(NOTIFICATION.SUBJECT.eq(subject))
                                .and(NOTIFICATION.CREATED_AT.gt(recent)))
        );
        if (!exists) {
            exists = dsl.fetchExists(
                    dsl.selectOne().from(NOTIFICATION_HISTORY)
                            .where(NOTIFICATION_HISTORY.MEMBER_ID.eq(memberId)
                                    .and(NOTIFICATION_HISTORY.SUBJECT.eq(subject))
                                    .and(NOTIFICATION_HISTORY.CREATED_AT.gt(recent)))
            );
        }
        if (exists) {
            return false;
        }
        dsl.insertInto(NOTIFICATION)
                .set(NOTIFICATION.MEMBER_ID, memberId)
                .set(NOTIFICATION.EMAIL, email)
                .set(NOTIFICATION.SUBJECT, subject)
                .set(NOTIFICATION.BODY, body)
                .set(NOTIFICATION.CREATED_AT, now)
                .execute();
        return true;
    }

    public List<NotificationRecord> findPending() {
        return dsl.selectFrom(NOTIFICATION)
                .orderBy(NOTIFICATION.CREATED_AT.desc())
                .fetch();
    }

    public List<NotificationHistoryRecord> findRecentHistory(int limit) {
        return dsl.selectFrom(NOTIFICATION_HISTORY)
                .orderBy(NOTIFICATION_HISTORY.CREATED_AT.desc())
                .limit(limit)
                .fetch();
    }

    public long countPending() {
        return dsl.fetchCount(NOTIFICATION);
    }
}
