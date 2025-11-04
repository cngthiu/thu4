package com.example.library.scheduler;

import com.example.library.jooq.enums.LoanStatus;
import com.example.library.service.NotificationService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.jooq.DSLContext;
import org.jooq.Record6;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.example.library.jooq.tables.Book.BOOK;
import static com.example.library.jooq.tables.Loan.LOAN;
import static com.example.library.jooq.tables.Member.MEMBER;

@Component
public class OverdueScheduler {
    private static final long FINE_PER_DAY = 5_000L;

    private final DSLContext dsl;
    private final NotificationService notificationService;
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    public OverdueScheduler(DSLContext dsl, NotificationService notificationService) {
        this.dsl = dsl;
        this.notificationService = notificationService;
        currencyFormat.setMaximumFractionDigits(0);
    }

    @Scheduled(cron = "0 20 00 * * *", zone = "Asia/Ho_Chi_Minh")
    public void markOverdueDaily() {
        LocalDateTime now = LocalDateTime.now();

        handleDueSoonLoans(now);
        handleOverdueLoans(now);
    }

    private long calculateLateDays(LocalDateTime due, LocalDateTime reference) {
        if (!reference.isAfter(due)) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(due.toLocalDate(), reference.toLocalDate());
        return Math.max(1, days);
    }

    private void handleDueSoonLoans(LocalDateTime now) {
        LocalDateTime threshold = now.plusDays(1);
        List<Record6<Long, LocalDateTime, String, Long, String, String>> dueSoon = dsl.select(
                        LOAN.LOAN_ID,
                        LOAN.DUE_DATE,
                        BOOK.TITLE,
                        LOAN.MEMBER_ID,
                        MEMBER.FULL_NAME,
                        MEMBER.EMAIL)
                .from(LOAN)
                .join(BOOK).on(LOAN.BOOK_ID.eq(BOOK.BOOK_ID))
                .join(MEMBER).on(LOAN.MEMBER_ID.eq(MEMBER.MEMBER_ID))
                .where(LOAN.RETURN_DATE.isNull()
                        .and(LOAN.STATUS.eq(LoanStatus.BORROWED))
                        .and(LOAN.DUE_DATE.gt(now))
                        .and(LOAN.DUE_DATE.le(threshold)))
                .fetch();

        for (Record6<Long, LocalDateTime, String, Long, String, String> record : dueSoon) {
            Long loanId = record.get(LOAN.LOAN_ID);
            LocalDateTime due = record.get(LOAN.DUE_DATE);
            String bookTitle = record.get(BOOK.TITLE);
            Long memberId = record.get(LOAN.MEMBER_ID);
            String memberName = record.get(MEMBER.FULL_NAME);
            String email = record.get(MEMBER.EMAIL);

            long daysRemaining = Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(), due.toLocalDate()));
            String subject = "[Library] Reminder loan #" + loanId + " due soon";
            String body = String.format(
                    Locale.ENGLISH,
                    "Xin chào %s,%n%n" +
                            "Phiếu mượn #%d cho cuốn sách \"%s\" sẽ đến hạn vào %s." +
                            " Còn %d ngày để trả sách.%n%n" +
                            "Vui lòng trả sách đúng hạn để tránh phát sinh phí phạt. Cảm ơn bạn!",
                    memberName,
                    loanId,
                    bookTitle,
                    due.toLocalDate(),
                    daysRemaining);

            notificationService.queueNotification(memberId, email, subject, body);
        }
    }

    private void handleOverdueLoans(LocalDateTime now) {
        List<Record6<Long, LocalDateTime, String, Long, String, String>> overdues = dsl.select(
                        LOAN.LOAN_ID,
                        LOAN.DUE_DATE,
                        BOOK.TITLE,
                        LOAN.MEMBER_ID,
                        MEMBER.FULL_NAME,
                        MEMBER.EMAIL)
                .from(LOAN)
                .join(BOOK).on(LOAN.BOOK_ID.eq(BOOK.BOOK_ID))
                .join(MEMBER).on(LOAN.MEMBER_ID.eq(MEMBER.MEMBER_ID))
                .where(LOAN.RETURN_DATE.isNull().and(LOAN.DUE_DATE.lt(now)))
                .fetch();

        for (Record6<Long, LocalDateTime, String, Long, String, String> record : overdues) {
            Long loanId = record.get(LOAN.LOAN_ID);
            LocalDateTime due = record.get(LOAN.DUE_DATE);
            String bookTitle = record.get(BOOK.TITLE);
            Long memberId = record.get(LOAN.MEMBER_ID);
            String memberName = record.get(MEMBER.FULL_NAME);
            String email = record.get(MEMBER.EMAIL);

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

            String subject = "[Library] Overdue loan #" + loanId;
            String body = String.format(
                    Locale.ENGLISH,
                    "Xin chào %s,%n%n" +
                            "Phiếu mượn #%d cho cuốn sách \"%s\" đã quá hạn %d ngày (đến hạn từ %s).%n" +
                            "Phí phạt hiện tại: %s VND.%n%n" +
                            "Vui lòng sắp xếp trả sách sớm nhất có thể. Cảm ơn bạn!",
                    memberName,
                    loanId,
                    bookTitle,
                    lateDays,
                    due.toLocalDate(),
                    currencyFormat.format(fine.longValue())
            );

            notificationService.queueNotification(memberId, email, subject, body);
        }
    }
}
