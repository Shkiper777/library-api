package com.example.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public Book create(BookRequest request) {
        Book book = new Book();
        apply(book, request);
        return repository.save(book);
    }

    @Transactional
    public Book update(Long id, BookRequest request) {
        Book book = findById(id);
        apply(book, request);
        return repository.save(book);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw notFound(id);
        }
        repository.deleteById(id);
    }

    /** Выдать один экземпляр (вызывается из loan-service). 409, если экземпляров нет. */
    @Transactional
    public Book takeCopy(Long id) {
        if (!repository.existsById(id)) {
            throw notFound(id);
        }
        if (repository.takeCopy(id) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No copies of book " + id + " available");
        }
        return findById(id);
    }

    /** Принять один экземпляр обратно (вызывается из loan-service). */
    @Transactional
    public Book returnCopy(Long id) {
        if (repository.returnCopy(id) == 0) {
            throw notFound(id);
        }
        return findById(id);
    }

    private static void apply(Book book, BookRequest request) {
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setPublishedYear(request.publishedYear());
        book.setCopies(request.copies());
    }

    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Book " + id + " not found");
    }
}
