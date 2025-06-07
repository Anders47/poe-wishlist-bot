package com.anders.poewishlist.db;

import java.util.*;

public class InMemoryWishlistStore implements WishlistStore {
    private final Map<String,List<String>> store = new HashMap<>();

    @Override
    public void clearAll() {
        store.clear();
    }

    @Override
    public void clearUser(String userId) {
        store.remove(userId);
    }

    @Override public void addWish(String userId, String itemName) {
        List<String> list = store.computeIfAbsent(userId, k->new ArrayList<>());

        // only add if it doesnt exist already
        if (!list.contains(itemName)) {
            list.add(itemName);
        }
    }

    @Override public List<String> getWishes(String userId) {
        return Collections.unmodifiableList(
                store.getOrDefault(userId, Collections.emptyList())
        );
    }
}