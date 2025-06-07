package com.anders.poewishlist.service;

import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.entities.Message;
import java.util.*;
import java.util.stream.*;

public class WishlistParser {
    private final UniqueItemMatcher matcher;

    public WishlistParser(UniqueItemMatcher matcher) {
        this.matcher = matcher;
    }

    public List<String> parse(List<Message> messages, String userId) {
        return messages.stream()
                // only messages from the syncing user
                .filter(m -> m.getAuthor() != null && m.getAuthor().getId().equals(userId))

                // take the raw text and split into individual lines/tokens
                .flatMap(m -> Arrays.stream(m.getContentRaw().split("[,\\r\\n]+")))

                // trim each part
                .map(String::trim)

                // drop blank lines, commands and bot confirmations
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.startsWith("!"))
                .filter(s -> !s.startsWith("✅"))

                // fuzzy-match to canonical uniques, drop non-matches
                .map(matcher::match)
                .filter(Objects::nonNull)

                // remove duplicates, preserve order
                .distinct()

                .collect(Collectors.toList());
    }
}