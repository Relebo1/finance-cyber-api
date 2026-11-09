package com.sekutlu.finance.transaction;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService service;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) { return service.create(request); }
    @GetMapping
    public Page<TransactionResponse> list(@RequestParam(required = false) String type,
            @RequestParam(required = false) String q, @RequestParam(required = false) LocalDate startDate, @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(type, q, startDate, endDate, page, size);
    }
    @GetMapping("/search")
    public List<TransactionResponse> search(@RequestParam String q) { return service.search(q); }
    @GetMapping("/summary") public TransactionService.Summary summary() { return service.summary(); }
    @GetMapping("/{id}") public TransactionResponse get(@PathVariable String id) { return service.get(id); }
    @PutMapping("/{id}") public TransactionResponse update(@PathVariable long id, @Valid @RequestBody TransactionRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable long id) { service.delete(id); }
}
