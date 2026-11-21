package com.example.reader;

import org.springframework.data.jpa.repository.JpaRepository;

/** Доступ к таблице readers в PostgreSQL. */
public interface ReaderRepository extends JpaRepository<Reader, Long> {
}
