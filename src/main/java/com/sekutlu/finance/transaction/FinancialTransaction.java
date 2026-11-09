package com.sekutlu.finance.transaction;

import com.sekutlu.finance.category.Category;
import com.sekutlu.finance.account.FinancialAccount;
import com.sekutlu.finance.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "financial_transactions")
@Getter @Setter
public class FinancialTransaction extends BaseEntity {
    @Column(nullable = false, length = 255)
    private String description;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private FinancialAccount account;
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
}
