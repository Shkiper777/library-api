package com.example.loan;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * Бизнес-логика выдачи и возврата. Здесь сервисы взаимодействуют между собой:
 * loan-service обращается к reader-service (есть ли читатель) и к catalog-service (есть ли экземпляр книги).
 * Удалённые вызовы не обёрнуты в транзакцию БД, чтобы не держать соединение с PostgreSQL во время HTTP-запросов.
 */
@Service
public class LoanService {

    private final LoanRepository repository;
    private final CatalogClient catalog;
    private final ReaderClient readers;

    public LoanService(LoanRepository repository, CatalogClient catalog, ReaderClient readers) {
        this.repository = repository;
        this.catalog = catalog;
        this.readers = readers;
    }

    public List<Loan> findAll() {
        return repository.findAll();
    }

    public Loan findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan " + id + " not found"));
    }

    /** Выдать книгу читателю. */
    public Loan issue(LoanRequest request) {
        // 1. reader-service: существует ли читатель (404, если нет)
        ReaderDto reader = readers.getReader(request.readerId());
        // 2. catalog-service: забрать один экземпляр (404, если нет книги; 409, если экземпляры закончились)
        BookDto book = catalog.takeCopy(request.bookId());
        // 3. записать выдачу в PostgreSQL; если запись не удалась, вернуть экземпляр в каталог
        try {
            Loan loan = new Loan();
            loan.setBookId(book.id());
            loan.setBookTitle(book.title());
            loan.setReaderId(reader.id());
            loan.setReaderName(reader.fullName());
            loan.setIssuedAt(Instant.now());
            return repository.save(loan);
        } catch (RuntimeException e) {
            catalog.returnCopy(book.id());
            throw e;
        }
    }

    /** Принять книгу обратно. */
    public Loan returnBook(Long id) {
        Loan loan = findById(id);
        if (loan.getReturnedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loan " + id + " is already returned");
        }
        catalog.returnCopy(loan.getBookId());
        loan.setReturnedAt(Instant.now());
        return repository.save(loan);
    }
}
