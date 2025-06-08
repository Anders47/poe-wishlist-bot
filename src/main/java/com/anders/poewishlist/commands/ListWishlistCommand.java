package com.anders.poewishlist.commands;

import com.anders.poewishlist.db.WishlistStore;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ListWishlistCommand extends ListenerAdapter {
    private static final String COMMAND = "!listwishlist";
    private final WishlistStore store;
    private static final Logger log = LoggerFactory.getLogger(ListWishlistCommand.class);


    public ListWishlistCommand(WishlistStore store) {
        this.store = store;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // only respond to real users in guild text‐channels
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;
        if (!(event.getChannel() instanceof TextChannel channel)) return;

        String content = event.getMessage().getContentRaw().trim();
        if (!content.equalsIgnoreCase(COMMAND)) return;

        String userId = event.getAuthor().getId();
        List<String> wishes = store.getWishes(userId);

        if (wishes.isEmpty()) {
            channel.sendMessage("Your wishlist is empty!").queue();
        } else {
            String body = wishes.stream()
                    .map(item -> "• " + item)
                    .collect(Collectors.joining("\n"));
            channel.sendMessage("Your wishlist:\n" + body).queue();
            log.info("Wishlist for user {}: \n {}", userId, body);
        }
    }
}