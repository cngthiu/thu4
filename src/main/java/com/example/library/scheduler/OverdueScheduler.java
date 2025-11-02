package com.example.library.scheduler;

import com.example.library.jooq.enums.LoanStatus;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.example.library.jooq.tables.Book.BOOK;
import static com.example.library.jooq.tables.Loan.LOAN;
import static com.example.library.jooq.tables.Member.MEMBER;
import static com.example.library.jooq.tables.Notification.NOTIFICATION;

@Component
public class OverdueScheduler {
    private static final long FINE_PER_DAY = 5_000L;

    private final DSLContext dsl;
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    public OverdueScheduler(DSLContext dsl) {
        this.dsl = dsl;
        currencyFormat.setMaximumFractionDigits(0);
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    public void markOverdueDaily() {
        LocalDateTime now = LocalDateTime.now();
        List<Record4<Long, Long, LocalDateTime, String>> overdues = dsl.select(
                        LOAN.LOAN_ID,
                        LOAN.MEMBER_ID,
                        LOAN.DUE_DATE,
                        BOOK.TITLE)
                .from(LOAN)
                .join(BOOK).on(LOAN.BOOK_ID.eq(BOOK.BOOK_ID))
                .where(LOAN.RETURN_DATE.isNull().and(LOAN.DUE_DATE.lt(now)))
                .fetch();

        for (Record4<Long, Long, LocalDateTime, String> record : overdues) {
            Long loanId = record.get(LOAN.LOAN_ID);
            Long memberId = record.get(LOAN.MEMBER_ID);
            LocalDateTime due = record.get(LOAN.DUE_DATE);
            String bookTitle = record.get(BOOK.TITLE);

            long lateDays = calculateLateDays(due, now);
            if (lateDays <= 0) {
                continue;
            }
            BigDecimal fine = BigDecimal.valueOf(lateDays * FINE_PER_DAY);

            dsl.update(LOAN)
                    .set(LOAN.STATUS, LoanStatus.OVERDUE)
                    .set(LOAN.FINE_AMOUNT, fine)
                    .where(LOAN.LOAN_ID.eq(loanId))
                    .execute();

            var member = dsl.select(MEMBER.EMAIL, MEMBER.FULL_NAME)
                    .from(MEMBER)
                    .where(MEMBER.MEMBER_ID.eq(memberId))
                    .fetchOne();
            if (member == null) {
                continue;
            }

            String email = member.get(MEMBER.EMAIL);
            String subject = "[Library] Overdue loan #" + loanId;
            String body = String.format(
                    Locale.ENGLISH,
                    "Xin chào %s,%n%n" +
                            "Phiếu mượn #%d cho cuốn sách \"%s\" đã quá hạn %d ngày (đến hạn từ %s).%n" +
                            "Phí phạt hiện tại: %s VND.%n%n" +
                            "Vui lòng sắp xếp trả sách sớm nhất có thể. Cảm ơn bạn!",
                    member.get(MEMBER.FULL_NAME),
                    loanId,
                    bookTitle,
                    lateDays,
                    due.toLocalDate(),
                    currencyFormat.format(fine.longValue())
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

    private long calculateLateDays(LocalDateTime due, LocalDateTime reference) {
        if (!reference.isAfter(due)) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(due.toLocalDate(), reference.toLocalDate());
        return Math.max(1, days);
    }
}
