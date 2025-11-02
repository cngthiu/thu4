package com.example.library.service;

import com.example.library.jooq.enums.LoanStatus;
import com.example.library.jooq.tables.records.LoanRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.library.jooq.tables.Book.BOOK;
import static com.example.library.jooq.tables.Loan.LOAN;

@Service
public class LoanService {
    private final DSLContext dsl;

    public LoanService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public Long borrow(Long bookId, Long memberId, int days) {
        var book = dsl.selectFrom(BOOK)
                .where(BOOK.BOOK_ID.eq(bookId))
                .forUpdate()
                .fetchOne();
        if (book == null) {
            throw new IllegalArgumentException("Book not found: " + bookId);
        }
        Integer stock = book.getStock();
        if (stock == null || stock <= 0) {
            throw new IllegalArgumentException("Book out of stock");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = now.plusDays(Math.max(1, days));
        var loanRecord = dsl.newRecord(LOAN);
        loanRecord.setBookId(bookId);
        loanRecord.setMemberId(memberId);
        loanRecord.setBorrowDate(now);
        loanRecord.setDueDate(due);
        loanRecord.setStatus(LoanStatus.BORROWED);
        loanRecord.setFineAmount(BigDecimal.ZERO);
        loanRecord.store();
        dsl.update(BOOK)
                .set(BOOK.STOCK, stock - 1)
                .where(BOOK.BOOK_ID.eq(bookId))
                .execute();
        return loanRecord.getLoanId();
    }

    @Transactional
    public void returnBook(Long loanId) {
        LoanRecord loan = dsl.selectFrom(LOAN)
                .where(LOAN.LOAN_ID.eq(loanId))
                .forUpdate()
                .fetchOne();
        if (loan == null) {
            throw new IllegalArgumentException("Loan not found: " + loanId);
        }
        if (LoanStatus.RETURNED.equals(loan.getStatus())) {
            return;
        }
        loan.setReturnDate(LocalDateTime.now());
        loan.setStatus(LoanStatus.RETURNED);
        loan.store();
        dsl.update(BOOK)
                .set(BOOK.STOCK, BOOK.STOCK.plus(1))
                .where(BOOK.BOOK_ID.eq(loan.getBookId()))
                .execute();
    }

    public Result<LoanRecord> list(String q, String status, int page, int size) {
        Condition condition = DSL.trueCondition();
        LoanStatus desiredStatus = parseStatus(status);
        if (desiredStatus != null) {
            condition = condition.and(LOAN.STATUS.eq(desiredStatus));
        }
        return dsl.selectFrom(LOAN)
                .where(condition)
                .orderBy(LOAN.DUE_DATE.asc())
                .limit(size)
                .offset(page * size)
                .fetch();
    }

    private LoanStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return LoanStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid loan status: " + status, ex);
        }
    }
}
