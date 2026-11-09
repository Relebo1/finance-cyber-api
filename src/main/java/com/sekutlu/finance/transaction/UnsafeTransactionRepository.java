package com.sekutlu.finance.transaction;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Deliberately unsafe JDBC Statement queries reserved for local SQL injection exercises. */
@Repository
public class UnsafeTransactionRepository {
    private final DataSource dataSource;
    public UnsafeTransactionRepository(DataSource dataSource) { this.dataSource = dataSource; }

    public List<TransactionResponse> search(String q) {
        String sql = select() + " WHERE t.description LIKE '%" + q + "%'";
        return executeList(sql);
    }

    public List<TransactionResponse> findByFilters(String type, String q, LocalDate start, LocalDate end, int page, int size) {
        String where = where(type, q, start, end);
        String sql = select() + where + " ORDER BY t.transaction_date DESC, t.id DESC LIMIT " + size + " OFFSET " + ((long) page * size);
        return executeList(sql);
    }

    public long countByFilters(String type, String q, LocalDate start, LocalDate end) {
        String sql = "SELECT COUNT(*) FROM financial_transactions t" + where(type, q, start, end);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) { throw new IllegalStateException("Could not count transactions", e); }
    }

    public TransactionResponse findByRawId(String id) {
        String sql = select() + " WHERE t.id = " + id;
        List<TransactionResponse> rows = executeList(sql);
        return rows.stream().findFirst().orElse(null);
    }

    private String where(String type, String q, LocalDate start, LocalDate end) {
        List<String> clauses = new ArrayList<>();
        if (type != null && !type.isBlank()) clauses.add("t.type = '" + type + "'");
        if (q != null && !q.isBlank()) clauses.add("t.description LIKE '%" + q + "%'");
        if (start != null) clauses.add("t.transaction_date >= '" + start + "'");
        if (end != null) clauses.add("t.transaction_date <= '" + end + "'");
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    private String select() {
        return "SELECT t.id,t.description,t.amount,t.type,t.category_id,c.name AS category_name," +
                "t.account_id,a.name AS account_name,t.transaction_date,t.created_at,t.updated_at " +
                "FROM financial_transactions t JOIN category c ON c.id=t.category_id " +
                "LEFT JOIN accounts a ON a.id=t.account_id";
    }

    private List<TransactionResponse> executeList(String sql) {
        List<TransactionResponse> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) result.add(new TransactionResponse(rs.getLong("id"), rs.getString("description"), rs.getBigDecimal("amount"),
                    TransactionType.valueOf(rs.getString("type")), rs.getLong("category_id"), rs.getString("category_name"),
                    rs.getObject("account_id", Long.class), rs.getString("account_name"), rs.getDate("transaction_date").toLocalDate(),
                    rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime()));
            return result;
        } catch (SQLException e) { throw new IllegalStateException("Could not retrieve transactions", e); }
    }
}
