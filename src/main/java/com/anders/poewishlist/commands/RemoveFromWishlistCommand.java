package com.anders.poewishlist.commands;
import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.util.UniqueItemMatcher;
import com.anders.poewishlist.service.WishlistParser;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;


public class RemoveFromWishlistCommand extends ListenerAdapter {
    private static final String PREFIX = "!remove ";
    private final WishlistStore store;
    private final UniqueItemMatcher matcher;
    private WishlistParser parser;
    private static final Logger log = LoggerFactory.getLogger(RemoveFromWishlistCommand.class);


    public RemoveFromWishlistCommand(WishlistStore store, UniqueItemMatcher matcher, WishlistParser parser) {
        this.store   = store;
        this.matcher = matcher;
        this.parser = parser;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String raw     = event.getMessage().getContentRaw().trim();
        if (!raw.toLowerCase().startsWith(PREFIX)) return;

        String userId  = event.getAuthor().getId();
        String payload = raw.substring(PREFIX.length()).trim();

        List<String> requested = parser.parseText(payload);
        if (requested.isEmpty()) {
            event.getChannel()
                    .sendMessage("❌ No valid uniques found to remove.")
                    .queue();
            log.info("User {} tried to remove {} unrecognized unique items, specifically :{}", userId, requested.size(), requested);
            return;
        }

        // fetch current
        List<String> current = store.getWishes(userId);

        // partition
        List<String> toRemove = requested.stream()
                .filter(current::contains)
                .toList();
        List<String> missing  = requested.stream()
                .filter(item -> !current.contains(item))
                .toList();

        // remove the ones they actually have
        toRemove.forEach(item -> store.removeWish(userId, item));

        // build responses
        if (!toRemove.isEmpty()) {
            String rem = toRemove.stream().map(i->"**"+i+"**").collect(Collectors.joining(", "));
            event.getChannel()
                    .sendMessage("✅ Removed " + rem + " from your wishlist.")
                    .queue();
            log.info("User {} removed {} items from their wishlist, specifically {}", userId, toRemove.size(), toRemove);
        }
        if (!missing.isEmpty()) {
            String miss = missing.stream().map(i->"**"+i+"**").collect(Collectors.joining(", "));
            event.getChannel()
                    .sendMessage("⚠️ You didn’t have " + miss + " on your wishlist; nothing to remove.")
                    .queue();
            log.info("User {} tried to remove {} from their wishlist, but it was empty", userId, miss);
        }
    }
}