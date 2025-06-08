package com.anders.poewishlist.commands;

import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddToWishlistCommand extends ListenerAdapter {
    private final WishlistStore store;
    private final UniqueItemMatcher matcher;
    private static final Logger log = LoggerFactory.getLogger(AddToWishlistCommand.class);

    public AddToWishlistCommand(WishlistStore store, UniqueItemMatcher matcher) {
        this.store   = store;
        this.matcher = matcher;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentRaw().trim();
        if (!content.toLowerCase().startsWith("!add ")) return;

        String raw = content.substring(5).trim();
        String canonical = matcher.match(raw);
        String user = event.getAuthor().getId();
        if (canonical == null) {
            event.getChannel()
                    .sendMessage("❌ Could not recognize item `" + raw + "`.").queue();
            log.info("User {} tried to add {} unrecognized item", user, raw);
            return;
        }

        store.addWish(user, canonical);
        event.getChannel()
                .sendMessage("✅ Added **" + canonical + "** to your wishlist.").queue();
        log.info("User {} added {} to their wishlist", user, canonical);
    }
}
