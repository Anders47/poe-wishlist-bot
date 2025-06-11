package com.anders.poewishlist;

import com.anders.poewishlist.commands.*;
import com.anders.poewishlist.db.DatabaseWishlistStore;
import com.anders.poewishlist.db.InMemoryWishlistStore;
import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.service.WishlistParser;
import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bot {
    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    public static void main(String[] args) throws Exception {
        // Load token from env var
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("DISCORD_TOKEN not set – aborting startup.");
            return;
        }
        log.info("Starting PoE Wishlist Bot…");
        String jdbcUrl = "jdbc:sqlite:poewishlist.db";

        // Wire up our store, parser & matcher
        WishlistStore store        = new DatabaseWishlistStore(jdbcUrl);
        UniqueItemMatcher matcher  = new UniqueItemMatcher();
        WishlistParser parser      = new WishlistParser(matcher);

        // Build and configure JDA
        JDABuilder builder = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(
                        new PingCommand(),
                        new ListWishlistCommand(store),
                        new AddToWishlistCommand(store, matcher, parser),
                        new RemoveFromWishlistCommand(store, matcher, parser),
                        new ClearWishlistCommand(store)
                );

        // Launch the bot
        builder.build();
        log.info("Bot is up and running!");
    }
}