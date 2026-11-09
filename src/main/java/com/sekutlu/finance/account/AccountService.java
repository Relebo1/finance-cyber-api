package com.sekutlu.finance.account;

import com.sekutlu.finance.transaction.ResourceNotFoundException;
import com.sekutlu.finance.transaction.TransactionRepository;
import com.sekutlu.finance.transaction.ResourceConflictException;
import com.sekutlu.finance.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accounts;
    private final AppUserRepository users;
    private final TransactionRepository transactions;

    @Transactional
    public AccountView create(String name, String currency, Long userId) {
        var user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var a = new FinancialAccount(); a.setUser(user); a.setName(name); a.setCurrency(currency == null ? "USD" : currency);
        return AccountView.from(accounts.save(a));
    }
    @Transactional(readOnly = true)
    public List<AccountView> list(Long userId) { return accounts.findByUserId(userId).stream().map(AccountView::from).toList(); }
    @Transactional(readOnly = true)
    public AccountView get(long id) { return AccountView.from(find(id)); }
    @Transactional
    public AccountView update(long id, String name, String currency) {
        var a = find(id); a.setName(name); a.setCurrency(currency); return AccountView.from(accounts.save(a));
    }
    @Transactional
    public void delete(long id) {
        var a = find(id);
        if (transactions.countByAccount_Id(id) > 0) throw new ResourceConflictException("Account is linked to transactions");
        accounts.delete(a);
    }
    private FinancialAccount find(long id) { return accounts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Account " + id + " not found")); }
    public record AccountView(Long id, String name, String currency, Long userId) {
        static AccountView from(FinancialAccount a) { return new AccountView(a.getId(), a.getName(), a.getCurrency(), a.getUser().getId()); }
    }
}
