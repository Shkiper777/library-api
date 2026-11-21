package com.example.loan;

import org.springframework.data.jpa.repository.JpaRepository;

/** Доступ к таблице loans в PostgreSQL. */
public interface LoanRepository extends JpaRepository<Loan, Long> {
}
