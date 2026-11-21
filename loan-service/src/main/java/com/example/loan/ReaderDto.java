package com.example.loan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Читатель так, как его отдаёт reader-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReaderDto(Long id, String fullName, String email) {
}
