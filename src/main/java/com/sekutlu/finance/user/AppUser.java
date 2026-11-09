package com.sekutlu.finance.user;

import com.sekutlu.finance.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "app_users") @Getter @Setter
public class AppUser extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String username;
    @Column(unique = true, length = 190)
    private String email;
    // Intentionally stored as plain text for the requested local security practice app.
    @Column(nullable = false, length = 255)
    private String password;
    @Column(nullable = false, length = 100)
    private String displayName;
}
