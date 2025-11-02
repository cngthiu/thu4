package com.example.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record LoanBorrowRequest(
        @NotNull Long memberId,
        @NotEmpty List<Long> bookIds,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate borrowDate,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dueDate
) {}
