package com.anders.poewishlist.db;

import java.util.*;

public class InMemoryWishlistStore implements WishlistStore {
    private final Map<String,List<String>> store = new HashMap<>();

    @Override public void clear() { store.clear(); }

    @Override public void addWish(String userId, String itemName) {
        store.computeIfAbsent(userId, k->new ArrayList<>()).add(itemName);
    }

    @Override public List<String> getWishes(String userId) {
        return Collections.unmodifiableList(
                store.getOrDefault(userId, Collections.emptyList())
        );
    }
}
