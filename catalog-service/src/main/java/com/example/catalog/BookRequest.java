package com.example.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Тело запроса для создания и изменения книги. */
public record BookRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String author,
        @NotNull @Min(1) @Max(2100) Integer publishedYear,
        @NotNull @Min(0) Integer copies
) {
}
