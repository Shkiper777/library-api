package com.example.library;

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
