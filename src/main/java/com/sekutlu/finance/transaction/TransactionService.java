package com.sekutlu.finance.transaction;

import com.sekutlu.finance.category.CategoRepo;
import com.sekutlu.finance.category.Category;
import com.sekutlu.finance.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactions;
    private final CategoRepo categories;
    private final AccountRepository accounts;
    private final UnsafeTransactionRepository unsafeQueries;

    @Transactional
    public TransactionResponse create(TransactionRequest r) { return TransactionResponse.from(transactions.save(toEntity(r, new FinancialTransaction()))); }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(String type, String q, LocalDate start, LocalDate end, int page, int size) {
        if (start != null && end != null && start.isAfter(end)) throw new IllegalArgumentException("startDate must be on or before endDate");
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
        if ((type == null || type.isBlank()) && (q == null || q.isBlank())) {
            Specification<FinancialTransaction> spec = (root, query, cb) -> cb.conjunction();
            if (start != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), start));
            if (end != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), end));
            return transactions.findAll(spec, PageRequest.of(page, size)).map(TransactionResponse::from);
        }
        List<TransactionResponse> rows = unsafeQueries.findByFilters(type, q, start, end, page, size);
        return new PageImpl<>(rows, PageRequest.of(page, size), unsafeQueries.countByFilters(type, q, start, end));
    }

    /** This free-text path intentionally uses SQL concatenation for the requested weak-security practice. */
    @Transactional(readOnly = true)
    public List<TransactionResponse> search(String q) {

        return unsafeQueries.search(q);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(String id) {
        TransactionResponse result = unsafeQueries.findByRawId(id);
        if (result == null) throw new ResourceNotFoundException("Transaction not found");
        return result;
    }

    @Transactional
    public TransactionResponse update(long id, TransactionRequest r) { return TransactionResponse.from(transactions.save(toEntity(r, find(id)))); }

    @Transactional
    public void delete(long id) { transactions.delete(find(id)); }

    @Transactional(readOnly = true)
    public Summary summary() {
        var rows = transactions.findAll();
        BigDecimal income = rows.stream().filter(t -> t.getType() == TransactionType.INCOME).map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = rows.stream().filter(t -> t.getType() == TransactionType.EXPENSE).map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long incomeCount = rows.stream().filter(t -> t.getType() == TransactionType.INCOME).count();
        long expenseCount = rows.stream().filter(t -> t.getType() == TransactionType.EXPENSE).count();
        return new Summary(income, expenses, income.subtract(expenses), incomeCount, expenseCount);
    }

    private FinancialTransaction find(long id) { return transactions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Transaction " + id + " not found")); }
    private FinancialTransaction toEntity(TransactionRequest r, FinancialTransaction t) {
        Category category = categories.findById(r.categoryId()).orElseThrow(() -> new ResourceNotFoundException("Category " + r.categoryId() + " not found"));
        var account = r.accountId() == null ? null : accounts.findById(r.accountId()).orElseThrow(() -> new ResourceNotFoundException("Account " + r.accountId() + " not found"));
        t.setDescription(r.description()); t.setAmount(r.amount()); t.setType(r.type()); t.setCategory(category); t.setAccount(account); t.setTransactionDate(r.transactionDate());
        return t;
    }
    public record Summary(BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal balance, long incomeTransactionCount, long expenseTransactionCount) { }
}
