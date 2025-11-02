package com.example.library.dto;

import com.example.library.jooq.enums.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanListItem(
        Long loanId,
        Long memberId,
        String memberName,
        Long bookId,
        String bookTitle,
        LocalDateTime borrowDate,
        LocalDateTime dueDate,
        LocalDateTime returnDate,
        LoanStatus status,
        BigDecimal fineAmount
) {}
