package com.example.library.scheduler;

import com.example.library.jooq.enums.LoanStatus;
import com.example.library.jooq.tables.records.LoanRecord;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.example.library.jooq.tables.Loan.LOAN;
import static com.example.library.jooq.tables.Member.MEMBER;
import static com.example.library.jooq.tables.Notification.NOTIFICATION;

@Component
public class OverdueScheduler {
    private final DSLContext dsl;

    public OverdueScheduler(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    public void markOverdueDaily() {
        LocalDateTime now = LocalDateTime.now();
        List<LoanRecord> overdues = dsl.selectFrom(LOAN)
                .where(LOAN.RETURN_DATE.isNull().and(LOAN.DUE_DATE.lt(now)))
                .fetch();
        for (LoanRecord loan : overdues) {
            LocalDateTime due = loan.getDueDate();
            long daysLate = Math.max(0, Duration.between(due, now).toDays());
            BigDecimal fine = BigDecimal.valueOf(daysLate * 5000L);
            dsl.update(LOAN)
                    .set(LOAN.STATUS, LoanStatus.OVERDUE)
                    .set(LOAN.FINE_AMOUNT, fine)
                    .where(LOAN.LOAN_ID.eq(loan.getLoanId()))
                    .execute();

            Long memberId = loan.getMemberId();
            Record2<String, String> member = dsl.select(MEMBER.EMAIL, MEMBER.FULL_NAME)
                    .from(MEMBER)
                    .where(MEMBER.MEMBER_ID.eq(memberId))
                    .fetchOne();
            if (member == null) {
                continue;
            }
            String email = member.get(MEMBER.EMAIL);
            String subject = "[Library] Overdue notice for your loan";
            String body = String.format(
                    "Dear %s,%nYour loan %d is overdue since %s. Fine: %s VND.",
                    member.get(MEMBER.FULL_NAME),
                    loan.getLoanId(),
                    due,
                    fine.toPlainString()
            );

            boolean exists = dsl.fetchExists(dsl.selectFrom(NOTIFICATION)
                    .where(NOTIFICATION.MEMBER_ID.eq(memberId)
                            .and(NOTIFICATION.SUBJECT.eq(subject))
                            .and(NOTIFICATION.CREATED_AT.gt(now.minusDays(1)))));
            if (!exists) {
                dsl.insertInto(NOTIFICATION)
                        .set(NOTIFICATION.MEMBER_ID, memberId)
                        .set(NOTIFICATION.EMAIL, email)
                        .set(NOTIFICATION.SUBJECT, subject)
                        .set(NOTIFICATION.BODY, body)
                        .execute();
            }
        }
    }
}
