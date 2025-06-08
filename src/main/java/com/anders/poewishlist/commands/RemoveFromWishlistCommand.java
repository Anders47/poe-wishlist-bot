package com.anders.poewishlist.commands;

import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveFromWishlistCommand extends ListenerAdapter {
    private static final String PREFIX = "!remove ";
    private final WishlistStore store;
    private final UniqueItemMatcher matcher;
    private static final Logger log = LoggerFactory.getLogger(RemoveFromWishlistCommand.class);


    public RemoveFromWishlistCommand(WishlistStore store, UniqueItemMatcher matcher) {
        this.store   = store;
        this.matcher = matcher;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String raw = event.getMessage().getContentRaw().trim();
        if (!raw.toLowerCase().startsWith(PREFIX)) return;

        String arg = raw.substring(PREFIX.length()).trim();
        String canonical = matcher.match(arg);
        if (canonical == null) {
            event.getChannel()
                    .sendMessage("❌ Could not recognize item `" + arg + "`.").queue();
            log.info("User tried to remove {} unrecognized item `{}`", event.getAuthor().getName(), arg);
            return;
        }

        String userId = event.getAuthor().getId();
        store.removeWish(userId, canonical);
        event.getChannel()
                .sendMessage(
                        String.format("✅ Removed **%s** from your wishlist.", canonical)
                ).queue();
        log.info("Removed {} from user {} wishlist", canonical, userId);
    }
}