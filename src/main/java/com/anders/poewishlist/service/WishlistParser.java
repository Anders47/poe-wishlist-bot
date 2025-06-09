package com.anders.poewishlist.service;

import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.entities.Message;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class WishlistParser {
    private final UniqueItemMatcher matcher;

    public WishlistParser(UniqueItemMatcher matcher) {
        this.matcher = matcher;
    }

    public List<String> parse(List<Message> messages, String userId) {
        return messages.stream()
                .filter(m -> {
                    m.getAuthor();
                    return m.getAuthor().getId().equals(userId);
                })
                // drop commands & confirmations before splitting
                .map(m -> m.getContentRaw().trim())
                .filter(s -> !s.startsWith("!") && !s.startsWith("✅") && !s.isEmpty())
                // now split & fuzzy-match via your new helper
                .flatMap(s -> parseText(s).stream())
                .distinct()  // in case same item appears in multiple messages
                .collect(Collectors.toList());
    }

    public List<String> parseText(String rawPayload) {
        // split on commas or newlines, trim, fuzzy-match, drop nulls, dedupe
        return Arrays.stream(rawPayload.split("[,\\r\\n]+"))
                .map(String::trim)
                .map(matcher::match)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}