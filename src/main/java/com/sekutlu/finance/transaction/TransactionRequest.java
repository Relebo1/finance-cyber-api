package com.sekutlu.finance.transaction;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotNull TransactionType type,
        @NotNull @Positive Long categoryId,
        @Positive Long accountId,
        @NotNull LocalDate transactionDate) { }
