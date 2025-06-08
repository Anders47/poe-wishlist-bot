package com.anders.poewishlist.commands;

import com.anders.poewishlist.db.WishlistStore;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClearWishlistCommand extends ListenerAdapter {
    private static final String COMMAND = "!clearwishlist";
    private final WishlistStore store;
    private static final Logger log = LoggerFactory.getLogger(SyncWishlistCommand.class);

    public ClearWishlistCommand(WishlistStore store) {
        this.store = store;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentRaw().trim();
        if (!content.equalsIgnoreCase(COMMAND)) return;

        String userId = event.getAuthor().getId();
        store.clearUser(userId);

        event.getChannel()
                .sendMessage("✅ Cleared your wishlist.")
                .queue();
                log.info("Cleared wishlist for {}", userId);
    }
}