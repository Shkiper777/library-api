package com.example.loan;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @GetMapping
    public List<Loan> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Loan get(@PathVariable Long id) {
        return service.findById(id);
    }

    /** Выдать книгу: {"bookId": 1, "readerId": 1}. */
    @PostMapping
    public ResponseEntity<Loan> issue(@Valid @RequestBody LoanRequest request) {
        Loan loan = service.issue(request);
        return ResponseEntity.created(URI.create("/loans/" + loan.getId())).body(loan);
    }

    /** Принять книгу обратно. */
    @PostMapping("/{id}/return")
    public Loan giveBack(@PathVariable Long id) {
        return service.returnBook(id);
    }
}
