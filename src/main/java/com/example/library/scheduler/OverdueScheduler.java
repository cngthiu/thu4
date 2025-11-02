//src/main/java/com/example/library/scheduler/OverdueScheduler.java
package com.example.library.scheduler;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import static com.example.library.jooq.tables.Loan.LOAN;
import static com.example.library.jooq.tables.Member.MEMBER;
import static com.example.library.jooq.tables.Notification.NOTIFICATION;

@Component
public class OverdueScheduler {
    private final DSLContext dsl;
    public OverdueScheduler(DSLContext dsl) { this.dsl = dsl; }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    public void markOverdueDaily() {
        LocalDateTime now = LocalDateTime.now();
        List<Record> overdues = dsl.selectFrom(LOAN)
                .where(LOAN.RETURN_DATE.isNull().and(LOAN.DUE_DATE.lt(now))).fetch();
        for (Record r : overdues) {
            LocalDateTime due = r.get(LOAN.DUE_DATE);
            long daysLate = Math.max(0, Duration.between(due, now).toDays());
            java.math.BigDecimal fine = BigDecimal.valueOf(daysLate * 5000L);
            dsl.update(LOAN).set(LOAN.STATUS, "OVERDUE").set(LOAN.FINE_AMOUNT, fine)
               .where(LOAN.LOAN_ID.eq(r.get(LOAN.LOAN_ID))).execute();

            Long memberId = r.get(LOAN.MEMBER_ID);
            var member = dsl.select(MEMBER.EMAIL, MEMBER.FULL_NAME)
                    .from(MEMBER).where(MEMBER.MEMBER_ID.eq(memberId)).fetchOne();
            String email = member.get(MEMBER.EMAIL);
            String subject = "[Library] Overdue notice for your loan";
            String body = String.format("Dear %s,%nYour loan %d is overdue since %s. Fine: %s VND.",
                    member.get(MEMBER.FULL_NAME), r.get(LOAN.LOAN_ID), due, fine.toPlainString());

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
