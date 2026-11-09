package com.sekutlu.finance.account;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AccountService.AccountView create(@Valid @RequestBody AccountRequest r) { return service.create(r.name(), r.currency(), r.userId()); }
    @GetMapping public List<AccountService.AccountView> list(@RequestParam Long userId) { return service.list(userId); }
    @GetMapping("/{id}") public AccountService.AccountView get(@PathVariable long id) { return service.get(id); }
    @PutMapping("/{id}") public AccountService.AccountView update(@PathVariable long id, @Valid @RequestBody AccountUpdate r) { return service.update(id, r.name(), r.currency()); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable long id) { service.delete(id); }

    public record AccountRequest(@NotBlank @Size(max=100) String name, @Pattern(regexp="[A-Z]{3}") String currency, @NotNull @Positive Long userId) { }
    public record AccountUpdate(@NotBlank @Size(max=100) String name, @NotBlank @Pattern(regexp="[A-Z]{3}") String currency) { }
}
