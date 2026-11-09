package com.sekutlu.finance.common;

import com.sekutlu.finance.account.AccountRepository;
import com.sekutlu.finance.account.FinancialAccount;
import com.sekutlu.finance.category.CategoRepo;
import com.sekutlu.finance.category.Category;
import com.sekutlu.finance.transaction.FinancialTransaction;
import com.sekutlu.finance.transaction.TransactionRepository;
import com.sekutlu.finance.transaction.TransactionType;
import com.sekutlu.finance.user.AppUser;
import com.sekutlu.finance.user.AppUserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
public class SampleDataInitializer {
    @Bean
    ApplicationRunner sampleData(CategoRepo categories, AccountRepository accounts, TransactionRepository transactions, AppUserRepository users) {
        return args -> {
            AppUser user = users.findByUsername("labuser").orElseGet(() -> {
                AppUser u = new AppUser(); u.setUsername("labuser"); u.setEmail("labuser@example.test");
                u.setDisplayName("Taylor Example"); u.setPassword("finance123"); return users.save(u);
            });
            List<String[]> categorySeeds = List.of(
                    new String[]{"Groceries", "Food and household shopping"}, new String[]{"Salary", "Employment income"},
                    new String[]{"Rent", "Monthly housing cost"}, new String[]{"Transport", "Public transit and fuel"},
                    new String[]{"Entertainment", "Leisure and subscriptions"});
            for (String[] seed : categorySeeds) if (categories.findByName(seed[0]).isEmpty()) {
                Category c = new Category(); c.setName(seed[0]); c.setDescription(seed[1]); categories.save(c);
            }
            List<FinancialAccount> userAccounts = accounts.findByUserId(user.getId());
            for (String name : List.of("Cash", "Bank Account", "Credit Card")) if (userAccounts.stream().noneMatch(a -> a.getName().equals(name))) {
                FinancialAccount a = new FinancialAccount(); a.setUser(user); a.setName(name); a.setCurrency("USD"); accounts.save(a);
            }
            if (transactions.count() == 0) {
                List<FinancialAccount> allAccounts = accounts.findByUserId(user.getId());
                String[][] seeds = {
                        {"Salary", "3200.00", "INCOME", "Salary", "Bank Account", "1"},
                        {"Weekly groceries", "86.42", "EXPENSE", "Groceries", "Bank Account", "2"},
                        {"Monthly rent", "1100.00", "EXPENSE", "Rent", "Bank Account", "3"},
                        {"Bus pass", "45.00", "EXPENSE", "Transport", "Cash", "4"},
                        {"Streaming subscription", "14.99", "EXPENSE", "Entertainment", "Credit Card", "5"},
                        {"Freelance design", "450.00", "INCOME", "Salary", "Bank Account", "6"},
                        {"Market groceries", "63.10", "EXPENSE", "Groceries", "Cash", "7"},
                        {"Taxi ride", "22.50", "EXPENSE", "Transport", "Credit Card", "8"},
                        {"Concert ticket", "78.00", "EXPENSE", "Entertainment", "Credit Card", "9"},
                        {"Salary", "3200.00", "INCOME", "Salary", "Bank Account", "10"}
                };
                for (String[] s : seeds) {
                    FinancialTransaction t = new FinancialTransaction(); t.setDescription(s[0]); t.setAmount(new BigDecimal(s[1]));
                    t.setType(TransactionType.valueOf(s[2])); t.setCategory(categories.findByName(s[3]).orElseThrow());
                    t.setAccount(allAccounts.stream().filter(a -> a.getName().equals(s[4])).findFirst().orElseThrow());
                    t.setTransactionDate(LocalDate.now().minusDays(Long.parseLong(s[5]))); transactions.save(t);
                }
            }
        };
    }
}
