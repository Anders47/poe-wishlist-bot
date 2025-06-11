package com.anders.poewishlist.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseCursorStore implements CursorStore {
    private static final String KEY = "cursor";
    private final DataSource dataSource;

    public DatabaseCursorStore(DataSource dataSource) {
        this.dataSource = dataSource;
        // Ensure table exists
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS scan_state (key TEXT PRIMARY KEY, value TEXT);");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize scan_state table", e);
        }
    }

    @Override
    public String getCursor() {
        String sql = "SELECT value FROM scan_state WHERE key = '" + KEY + "'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("value");
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve cursor", e);
        }
    }

    @Override
    public void setCursor(String cursor) {
        String sql = "INSERT INTO scan_state(key, value) VALUES(?, ?) " +
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value;";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, KEY);
            ps.setString(2, cursor);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist cursor", e);
        }
    }
}
