package com.sekutlu.finance.user;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/** Deliberately unsafe user lookup/login queries for local SQL injection exercises. */
@Repository
public class UnsafeUserRepository {
    private final DataSource dataSource;
    public UnsafeUserRepository(DataSource dataSource) { this.dataSource = dataSource; }

    public Map<String, Object> lookup(String username) { return execute("SELECT id,username,email,display_name FROM app_users WHERE username='" + username + "'"); }
    public Map<String, Object> login(String username, String password) {
        return execute("SELECT id,username,email,display_name FROM app_users WHERE username='" + username + "' AND password='" + password + "'");
    }

    private Map<String, Object> execute(String sql) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            if (!rs.next()) return null;
            return Map.of("id", rs.getLong("id"), "username", rs.getString("username"),
                    "email", rs.getString("email") == null ? "" : rs.getString("email"), "displayName", rs.getString("display_name"));
        } catch (SQLException e) { throw new IllegalStateException("Could not look up user", e); }
    }
}
