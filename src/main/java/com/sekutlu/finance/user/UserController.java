package com.sekutlu.finance.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController @RequiredArgsConstructor @RequestMapping("/api/users")
public class UserController {
    private final UserService users;

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public UserService.UserView register(@Valid @RequestBody RegisterRequest r) {
        return users.register(r.username(), r.email(), r.password(), r.displayName());
    }
    @GetMapping("/lookup") public Map<String, Object> lookup(@RequestParam String username) { return users.lookup(username); }
    public record RegisterRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_.-]{3,100}") String username,
            @Email @Size(max=190) String email, @NotBlank @Size(max=255) String password, @NotBlank @Size(max=100) String displayName) { }
}
