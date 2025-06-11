package com.anders.poewishlist.db;

import java.util.List;

public interface WishlistStore {
    void clearAll();

    void clearUser(String userId);

    void addWish(String userId, String itemName);

    void removeWish(String userId, String itemName);

    List<String> getWishes(String userId);

    List<String> getUsersWithItem(String itemName);
}