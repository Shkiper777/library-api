package com.example.reader;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReaderService {

    private final ReaderRepository repository;

    public ReaderService(ReaderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Reader> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Reader findById(Long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public Reader create(ReaderRequest request) {
        Reader reader = new Reader();
        apply(reader, request);
        return repository.save(reader);
    }

    @Transactional
    public Reader update(Long id, ReaderRequest request) {
        Reader reader = findById(id);
        apply(reader, request);
        return repository.save(reader);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw notFound(id);
        }
        repository.deleteById(id);
    }

    private static void apply(Reader reader, ReaderRequest request) {
        reader.setFullName(request.fullName());
        reader.setEmail(request.email());
    }

    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader " + id + " not found");
    }
}
