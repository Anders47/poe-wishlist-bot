package com.anders.poewishlist.commands;

import com.anders.poewishlist.db.WishlistStore;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;

public class SyncWishlistCommand extends ListenerAdapter {
    // local logging
    private static final Logger log = LoggerFactory.getLogger(SyncWishlistCommand.class);
    private final WishlistStore store;

    public SyncWishlistCommand(WishlistStore store) {
        this.store = store;
    }

    /**
     * Public API for tests: synchronizes the store from the given channel.
     */
    public CompletableFuture<Void> sync(TextChannel channel) {
        return channel.getHistory()
                .retrievePast(100)    // RestAction<List<Message>>
                .submit()             // CompletableFuture<List<Message>>
                .thenAccept(messages -> {
                    store.clear();
                    String userId = channel.getId();

                    for (Message msg : messages) {
                        String raw = msg.getContentRaw().trim();
                        // skip empty lines, commands and confirmations
                        if (raw.isEmpty() || raw.startsWith("!") || raw.startsWith("✅") || raw.startsWith("❌")) {
                            continue;
                        }
                        // split multi-line messages into individual lines
                        String[] parts = raw.split("\\R");
                        for (String part : parts) {
                            String wish = part.trim();
                            if (wish.isEmpty()) {
                                continue;
                            }
                            store.addWish(userId, wish);
                        }
                    }
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
        sync(channel)
                .thenRun(() -> {
                    String userId = channel.getId();
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
