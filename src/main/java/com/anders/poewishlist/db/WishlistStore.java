package com.anders.poewishlist.db;

import java.util.List;

public interface WishlistStore {
    void clear();
    void addWish(String userId, String itemName);
    List<String> getWishes(String userId);
}