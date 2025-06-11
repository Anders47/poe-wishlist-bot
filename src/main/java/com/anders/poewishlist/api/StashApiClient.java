package com.anders.poewishlist.api;

public interface StashApiClient {
    /**
     * Gets stash-feed from Path of Exile API.
     *
     * @param league      Name of league
     * @param nextChangeId Cursor for pagination (can be null at first call)
     * @return StashResponse with tabs + new nextChangeId
     * @throws ApiException on HTTP-error or parse-error
     */
    StashResponse fetch(String league, String nextChangeId) throws ApiException;
}
