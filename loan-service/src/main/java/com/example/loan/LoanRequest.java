package com.example.loan;

import jakarta.validation.constraints.NotNull;

/** Тело запроса на выдачу книги. */
public record LoanRequest(
        @NotNull Long bookId,
        @NotNull Long readerId
) {
}
