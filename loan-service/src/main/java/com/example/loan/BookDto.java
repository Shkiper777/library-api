package com.example.loan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Книга так, как её отдаёт catalog-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookDto(Long id, String title, String author, Integer publishedYear, Integer copies) {
}
