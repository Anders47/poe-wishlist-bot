package com.anders.poewishlist.commands;

import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AddToWishlistCommand extends ListenerAdapter {
    private final WishlistStore store;
    private final UniqueItemMatcher matcher;

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
        if (canonical == null) {
            event.getChannel()
                    .sendMessage("❌ Could not recognize item `" + raw + "`.").queue();
            return;
        }

        String user = event.getAuthor().getId();
        store.addWish(user, canonical);
        event.getChannel()
                .sendMessage("✅ Added **" + canonical + "** to your wishlist.").queue();
    }
}
