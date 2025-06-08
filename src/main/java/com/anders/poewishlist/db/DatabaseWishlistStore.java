package com.anders.poewishlist.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseWishlistStore implements WishlistStore, AutoCloseable {
    private final HikariDataSource ds;

    public DatabaseWishlistStore(String jdbcUrl) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setMaximumPoolSize(2);
        this.ds = new HikariDataSource(cfg);
        initSchema();
    }

    private void initSchema() {
        try (Connection c = ds.getConnection();
             Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS wishlist (
                  user_id TEXT NOT NULL,
                  item    TEXT NOT NULL,
                  PRIMARY KEY(user_id, item)
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize schema", e);
        }
    }

    @Override
    public void close() {
        ds.close();
    }

    @Override
    public void clearAll() {
        try (Connection c = ds.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM wishlist");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearUser(String userId) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM wishlist WHERE user_id = ?"
             )) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addWish(String userId, String itemName) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR IGNORE INTO wishlist(user_id,item) VALUES(?,?)"
             )) {
            ps.setString(1, userId);
            ps.setString(2, itemName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeWish(String userId, String itemName) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM wishlist WHERE user_id=? AND item=?"
             )) {
            ps.setString(1, userId);
            ps.setString(2, itemName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getWishes(String userId) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT item FROM wishlist WHERE user_id = ? ORDER BY item"
             )) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(rs.getString("item"));
                }
                return Collections.unmodifiableList(items);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}