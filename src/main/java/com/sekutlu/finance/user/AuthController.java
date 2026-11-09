package com.sekutlu.finance.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Credential query for practice only; it does not establish an authenticated session. */
@RestController @RequiredArgsConstructor @RequestMapping("/api/auth")
public class AuthController {
    private final UserService users;
    @PostMapping("/login") public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        return Map.of("success", true, "user", users.login(request.username(), request.password()));
    }
    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }
}
