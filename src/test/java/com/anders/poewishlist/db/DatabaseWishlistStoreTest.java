package com.anders.poewishlist.db;

import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class DatabaseWishlistStoreTest {
    private static Path tempFile;
    private DatabaseWishlistStore store;

    @BeforeAll
    static void createTempFile() throws Exception {
        tempFile = Files.createTempFile("wishlist-test-", ".db");
    }

    @AfterAll
    static void deleteTempFile() throws Exception {
        Files.deleteIfExists(tempFile);
    }

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempFile.toAbsolutePath();
        store = new DatabaseWishlistStore(url);
        store.clearAll();
    }

    @AfterEach
    void tearDown() throws Exception {
        store.close();
    }

    @Test
    void addWishAndGetWishes_roundtrip() {
        store.addWish("u1", "Headhunter");
        store.addWish("u1", "Goldrim");
        List<String> result = store.getWishes("u1");
        assertThat(result, contains("Goldrim", "Headhunter")); // alphabetical order
    }

    @Test
    void preventDuplicates() {
        store.addWish("u1", "Headhunter");
        store.addWish("u1", "Headhunter");
        List<String> result = store.getWishes("u1");
        assertThat(result, contains("Headhunter"));
    }

    @Test
    void clearUser_onlyDeletesThatUser() {
        store.addWish("u1", "A");
        store.addWish("u2", "B");
        store.clearUser("u1");
        assertThat(store.getWishes("u1"), is(empty()));
        assertThat(store.getWishes("u2"), contains("B"));
    }
}