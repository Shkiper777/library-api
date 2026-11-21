package com.example.library;

import org.springframework.data.jpa.repository.JpaRepository;

/** Доступ к таблице books в PostgreSQL. */
public interface BookRepository extends JpaRepository<Book, Long> {
}
