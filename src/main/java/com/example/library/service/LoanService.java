//src/main/java/com/example/library/service/LoanService.java
package com.example.library.service;

import com.example.library.dto.LoanListItem;
import com.example.library.jooq.enums.LoanStatus;
import com.example.library.jooq.tables.records.LoanRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.library.jooq.tables.Book.BOOK;
import static com.example.library.jooq.tables.Loan.LOAN;
import static com.example.library.jooq.tables.Member.MEMBER;

@Service
public class LoanService {
    private final DSLContext dsl;

    public LoanService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Transactional
    public List<Long> borrow(Long memberId, List<Long> requestedBookIds, LocalDate borrowDate, LocalDate dueDate) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member is required");
        }
        if (requestedBookIds == null || requestedBookIds.isEmpty()) {
            throw new IllegalArgumentException("At least one book must be selected");
        }
        Set<Long> bookIds = new LinkedHashSet<>(requestedBookIds);
        if (bookIds.size() != requestedBookIds.size()) {
            throw new IllegalArgumentException("Duplicate books detected in request");
        }

        LocalDateTime borrowAt = borrowDate == null ? LocalDateTime.now() : borrowDate.atStartOfDay();
        LocalDateTime dueAt = dueDate == null
                ? borrowAt.plusDays(14)
                : dueDate.atTime(LocalTime.MAX);

        if (!dueAt.isAfter(borrowAt)) {
            throw new IllegalArgumentException("Due date must be after borrow date");
        }

        rejectIfMemberHasOutstandingLoans(memberId);

        var books = dsl.selectFrom(BOOK)
                .where(BOOK.BOOK_ID.in(bookIds))
                .forUpdate()
                .fetch();
        if (books.size() != bookIds.size()) {
            throw new IllegalArgumentException("One or more selected books were not found");
        }

        List<Long> createdIds = new ArrayList<>();
        books.forEach(book -> {
            Integer stock = book.getStock();
            if (stock == null || stock <= 0) {
                throw new IllegalArgumentException("Book out of stock: " + book.getTitle());
            }
        });

        for (var book : books) {
            LoanRecord loanRecord = dsl.newRecord(LOAN);
            loanRecord.setBookId(book.getBookId());
            loanRecord.setMemberId(memberId);
            loanRecord.setBorrowDate(borrowAt);
            loanRecord.setDueDate(dueAt);
            loanRecord.setStatus(LoanStatus.BORROWED);
            loanRecord.setFineAmount(BigDecimal.ZERO);
            loanRecord.store();
            createdIds.add(loanRecord.getLoanId());

            dsl.update(BOOK)
                    .set(BOOK.STOCK, BOOK.STOCK.minus(1))
                    .where(BOOK.BOOK_ID.eq(book.getBookId()))
                    .execute();
        }
        return createdIds;
    }

    @Transactional
    public void returnBook(Long loanId) {
        returnBook(loanId, null);
    }

    @Transactional
    public void returnBook(Long loanId, LocalDate returnDate) {
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
        LocalDateTime returnedAt = returnDate == null
                ? LocalDateTime.now()
                : returnDate.atTime(LocalTime.now());
        loan.setReturnDate(returnedAt);
        loan.setStatus(LoanStatus.RETURNED);
        loan.store();

        dsl.update(BOOK)
                .set(BOOK.STOCK, BOOK.STOCK.plus(1))
                .where(BOOK.BOOK_ID.eq(loan.getBookId()))
                .execute();
    }

    public List<LoanListItem> list(String q, String status, int page, int size) {
        Condition condition = DSL.trueCondition();
        LoanStatus desiredStatus = parseStatus(status);
        if (desiredStatus != null) {
            condition = condition.and(LOAN.STATUS.eq(desiredStatus));
        }
        if (q != null && !q.isBlank()) {
            String keyword = "%" + q.trim() + "%";
            condition = condition.and(
                    BOOK.TITLE.likeIgnoreCase(keyword)
                            .or(MEMBER.FULL_NAME.likeIgnoreCase(keyword))
                            .or(MEMBER.EMAIL.likeIgnoreCase(keyword))
            );
        }
        return dsl.select(
                        LOAN.LOAN_ID,
                        LOAN.MEMBER_ID,
                        MEMBER.FULL_NAME,
                        LOAN.BOOK_ID,
                        BOOK.TITLE,
                        LOAN.BORROW_DATE,
                        LOAN.DUE_DATE,
                        LOAN.RETURN_DATE,
                        LOAN.STATUS,
                        LOAN.FINE_AMOUNT
                )
                .from(LOAN)
                .join(BOOK).on(LOAN.BOOK_ID.eq(BOOK.BOOK_ID))
                .join(MEMBER).on(LOAN.MEMBER_ID.eq(MEMBER.MEMBER_ID))
                .where(condition)
                .orderBy(LOAN.DUE_DATE.asc())
                .limit(size)
                .offset(page * size)
                .fetch(this::mapToLoanListItem);
    }

    private void rejectIfMemberHasOutstandingLoans(Long memberId) {
        boolean hasDebt = dsl.fetchExists(
                dsl.selectOne()
                        .from(LOAN)
                        .where(LOAN.MEMBER_ID.eq(memberId)
                                .and(LOAN.STATUS.ne(LoanStatus.RETURNED)))
        );
        if (hasDebt) {
            throw new IllegalStateException("Member has outstanding loans and cannot borrow more books");
        }
    }

    private LoanListItem mapToLoanListItem(Record record) {
        return new LoanListItem(
                record.get(LOAN.LOAN_ID),
                record.get(LOAN.MEMBER_ID),
                record.get(MEMBER.FULL_NAME),
                record.get(LOAN.BOOK_ID),
                record.get(BOOK.TITLE),
                record.get(LOAN.BORROW_DATE),
                record.get(LOAN.DUE_DATE),
                record.get(LOAN.RETURN_DATE),
                record.get(LOAN.STATUS),
                record.get(LOAN.FINE_AMOUNT)
        );
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
