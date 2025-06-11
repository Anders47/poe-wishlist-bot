package com.anders.poewishlist.db;

public interface CursorStore {
    /**
     * Retrieves the last saved cursor (next_change_id). Returns null if none.
     */
    String getCursor();

    /**
     * Persists the given cursor for next scan.
     */
    void setCursor(String cursor);
}