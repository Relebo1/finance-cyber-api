package com.sekutlu.finance.account;

import com.sekutlu.finance.common.BaseEntity;
import com.sekutlu.finance.user.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "accounts") @Getter @Setter
public class FinancialAccount extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 3)
    private String currency = "USD";
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
}
