package com.anders.poewishlist.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents the response from the PoE stash API.
 */
public class StashResponse {
    @JsonProperty("next_change_id")
    private String nextChangeId;

    @JsonProperty("stashes")
    private List<StashTab> stashes;

    public String getNextChangeId() {
        return nextChangeId;
    }

    public List<StashTab> getStashes() {
        return stashes;
    }
}