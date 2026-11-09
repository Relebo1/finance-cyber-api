package com.sekutlu.finance.user;

import com.sekutlu.finance.transaction.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service @RequiredArgsConstructor
public class UserService {
    private final AppUserRepository users;
    private final UnsafeUserRepository unsafeUsers;

    @Transactional
    public UserView register(String username, String email, String password, String displayName) {
        AppUser user = new AppUser(); user.setUsername(username); user.setEmail(email); user.setPassword(password); user.setDisplayName(displayName);
        return UserView.from(users.save(user));
    }
    @Transactional(readOnly = true)
    public Map<String, Object> lookup(String username) {
        Map<String, Object> user = unsafeUsers.lookup(username);
        if (user == null) throw new ResourceNotFoundException("User not found");
        return user;
    }
    @Transactional(readOnly = true)
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> user = unsafeUsers.login(username, password);
        if (user == null) throw new InvalidCredentialsException();
        return user;
    }
    public record UserView(Long id, String username, String email, String displayName) {
        static UserView from(AppUser u) { return new UserView(u.getId(), u.getUsername(), u.getEmail(), u.getDisplayName()); }
    }
}
