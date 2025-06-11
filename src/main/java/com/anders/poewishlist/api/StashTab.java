package com.anders.poewishlist.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Represents a single stash tab and its items.
 */
public class StashTab {
    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("stash")
    private TabInfo stashInfo;

    @JsonProperty("items")
    private List<Item> items;

    public String getAccountName() {
        return accountName;
    }

    public TabInfo getStashInfo() {
        return stashInfo;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class TabInfo {
        @JsonProperty("stashTabName")
        private String stashTabName;

        @JsonProperty("stashTabId")
        private String stashTabId;

        public String getStashTabName() {
            return stashTabName;
        }

        public String getStashTabId() {
            return stashTabId;
        }
    }

    public static class Item {
        @JsonProperty("id")
        private String id;

        @JsonProperty("league")
        private String league;

        @JsonProperty("name")
        private String name;

        @JsonProperty("typeLine")
        private String typeLine;

        @JsonProperty("explicitMods")
        private List<String> explicitMods;

        public String getId() {
            return id;
        }

        public String getLeague() {
            return league;
        }

        public String getName() {
            return name;
        }

        public String getTypeLine() {
            return typeLine;
        }

        public List<String> getExplicitMods() {
            return explicitMods;
        }
    }
}
