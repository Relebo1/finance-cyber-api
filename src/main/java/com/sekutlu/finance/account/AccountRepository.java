package com.sekutlu.finance.account;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountRepository extends JpaRepository<FinancialAccount, Long> {
    List<FinancialAccount> findByUserId(Long userId);
}
