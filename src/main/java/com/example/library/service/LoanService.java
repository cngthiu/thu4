//src/main/java/com/example/library/service/LoanService.java
package com.example.library.service;

import org.jooq.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static com.example.library.jooq.tables.Book.BOOK;
import static com.example.library.jooq.tables.Loan.LOAN;

@Service
public class LoanService {
    private final DSLContext dsl;
    public LoanService(DSLContext dsl) { this.dsl = dsl; }

    @Transactional
    public Long borrow(Long bookId, Long memberId, int days) {
        var b = dsl.selectFrom(BOOK).where(BOOK.BOOK_ID.eq(bookId)).forUpdate().fetchOne();
        if (b == null) throw new IllegalArgumentException("Book not found: " + bookId);
        if (b.getStock() <= 0) throw new IllegalArgumentException("Book out of stock");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = now.plusDays(Math.max(1, days));
        var rec = dsl.newRecord(LOAN);
        rec.setBookId(bookId);
        rec.setMemberId(memberId);
        rec.setBorrowDate(now);
        rec.setDueDate(due);
        rec.setStatus("BORROWED");
        rec.setFineAmount(BigDecimal.ZERO);
        rec.store();
        dsl.update(BOOK).set(BOOK.STOCK, b.getStock() - 1).where(BOOK.BOOK_ID.eq(bookId)).execute();
        return rec.getLoanId();
    }

    @Transactional
    public void returnBook(Long loanId) {
        var loan = dsl.selectFrom(LOAN).where(LOAN.LOAN_ID.eq(loanId)).forUpdate().fetchOne();
        if (loan == null) throw new IllegalArgumentException("Loan not found: " + loanId);
        if ("RETURNED".equals(loan.getStatus())) return;
        loan.setReturnDate(LocalDateTime.now());
        loan.setStatus("RETURNED");
        loan.store();
        dsl.update(BOOK).set(BOOK.STOCK, BOOK.STOCK.plus(1))
           .where(BOOK.BOOK_ID.eq(loan.getBookId())).execute();
    }

    public Result<Record> list(String q, String status, int page, int size) {
        Condition cond = DSL.trueCondition();
        if (status != null && !status.isBlank()) cond = cond.and(LOAN.STATUS.eq(status));
        return dsl.select().from(LOAN).where(cond)
                .orderBy(LOAN.DUE_DATE.asc())
                .limit(size).offset(page * size).fetch();
    }
}
