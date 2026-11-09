package com.sekutlu.finance.category;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Deliberately unsafe search query for local SQL injection exercises. */
@Repository
public class UnsafeCategoryRepository {
    private final DataSource dataSource;
    public UnsafeCategoryRepository(DataSource dataSource) { this.dataSource = dataSource; }

    public List<CategoryDto> search(String name) {
        String sql = "SELECT id,name,description,created_at,updated_at FROM category WHERE name LIKE '%" + name + "%'";
        List<CategoryDto> results = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                CategoryDto dto = new CategoryDto(); dto.setId(rs.getLong("id")); dto.setName(rs.getString("name"));
                dto.setDescription(rs.getString("description")); dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                dto.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime()); results.add(dto);
            }
            return results;
        } catch (SQLException e) { throw new IllegalStateException("Could not search categories", e); }
    }
}
