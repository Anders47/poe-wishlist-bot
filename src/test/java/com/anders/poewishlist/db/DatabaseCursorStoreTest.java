package com.anders.poewishlist.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseCursorStoreTest {
    private DataSource dataSource;
    private DatabaseCursorStore store;

    @BeforeEach
    void setUp() {
        // In-memory SQLite for tests
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite::memory:");
        this.dataSource = new HikariDataSource(config);
        this.store = new DatabaseCursorStore(dataSource);
    }

    @Test
    void getCursorReturnsNullWhenEmpty() {
        assertNull(store.getCursor(), "Expected null when no cursor is set");
    }

    @Test
    void setCursorAndGetCursorReturnsValue() {
        store.setCursor("abc123");
        assertEquals("abc123", store.getCursor(), "Cursor should match the value set");

        // Overwrite the cursor
        store.setCursor("def456");
        assertEquals("def456", store.getCursor(), "Cursor should be updated to the new value");
    }
}