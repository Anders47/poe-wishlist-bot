package com.anders.poewishlist.commands;
import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.service.WishlistParser;
import com.anders.poewishlist.util.UniqueItemMatcher;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;

public class AddToWishlistCommand extends ListenerAdapter {
    private final WishlistStore store;
    private final UniqueItemMatcher matcher;
    private WishlistParser parser;
    private static final String PREFIX = "!add";
    private static final Logger log = LoggerFactory.getLogger(AddToWishlistCommand.class);

    public AddToWishlistCommand(WishlistStore store, UniqueItemMatcher matcher, WishlistParser parser) {
        this.store = store;
        this.matcher = matcher;
        this.parser = parser;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String content = event.getMessage().getContentRaw().trim();
        if (!content.toLowerCase().startsWith(PREFIX)) return;

        String userId = event.getAuthor().getId();
        String payload = content.substring(PREFIX.length()).trim();

        // parse multiple comma- or newline-separated entries
        List<String> requested = parser.parseText(payload);
        if (requested.isEmpty()) {
            event.getChannel()
                    .sendMessage("❌ No valid uniques found in your input.")
                    .queue();
            log.info("User {} tried to add {} unrecognized item", userId, requested);
            return;
        }

        List<String> current = store.getWishes(userId);

        List<String> toAdd = requested.stream()
                .filter(item -> !current.contains(item))
                .toList();

        List<String> already = requested.stream()
                .filter(current::contains)
                .toList();

        toAdd.forEach(item -> store.addWish(userId, item));

        // build responses
        if (!toAdd.isEmpty()) {
            String added = toAdd.stream().map(i -> "**" + i + "**").collect(Collectors.joining(", "));
            event.getChannel()
                    .sendMessage("✅ Added " + added + " to your wishlist.")
                    .queue();
            log.info("User {} added {} item to their wishlist", userId, toAdd);
        }
        if (!already.isEmpty()) {
            String dup = already.stream().map(i -> "**" + i + "**").collect(Collectors.joining(", "));
            event.getChannel()
                    .sendMessage("⚠️ You already had " + dup + " on your wishlist; skipped.")
                    .queue();
            log.info("User {} tried to add {} duplicate item to their wishlist", userId, payload);
        }
    }
}


