package com.anders.poewishlist.commands;

import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.anders.poewishlist.service.WishlistParser;
import java.util.List;

import java.util.concurrent.CompletableFuture;

public class SyncWishlistCommand extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(SyncWishlistCommand.class);
    private final WishlistStore store;
    private final WishlistParser parser;


    public SyncWishlistCommand(WishlistStore store, WishlistParser parser) {
        this.store = store;
        this.parser = parser;
    }

    /**
     * Public API for tests: synchronizes the store from the given channel.
     */
    public CompletableFuture<Void> sync(TextChannel channel, String userId) {
        return channel.getHistory().retrievePast(100).submit().thenAccept(messages -> {
            store.clearUser(userId);
            List<String> wishes = parser.parse(messages, userId);
            wishes.forEach(item -> store.addWish(userId, item));
        });
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;
        if (!(event.getChannel() instanceof TextChannel channel)) return;

        if (!channel.getName().equals("poe-wishlist")) return;

        String content = event.getMessage().getContentRaw().trim();
        if (!content.equalsIgnoreCase("!syncwishlist")) return;

        // Kick off the same sync logic, then send confirmation
        String userId = event.getAuthor().getId();
        sync(channel, userId)
                .thenRun(() -> {
                    int count = store.getWishes(userId).size();
                    channel.sendMessage(
                            String.format("✅ Wishlist synchronized! Loaded %d items.", count)
                    ).queue();
                    log.info("User {} synced {} items", userId, count);
                })
                .exceptionally(err -> {
                    channel.sendMessage("❌ Sync failed: " + err.getMessage()).queue();
                    return null;
                });
    }
}