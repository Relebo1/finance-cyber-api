package com.sekutlu.finance.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(Long id, String description, BigDecimal amount,
        TransactionType type, Long categoryId, String categoryName, Long accountId, String accountName, LocalDate transactionDate,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    static TransactionResponse from(FinancialTransaction t) {
        return new TransactionResponse(t.getId(), t.getDescription(), t.getAmount(), t.getType(),
                t.getCategory().getId(), t.getCategory().getName(), t.getAccount() == null ? null : t.getAccount().getId(),
                t.getAccount() == null ? null : t.getAccount().getName(), t.getTransactionDate(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
