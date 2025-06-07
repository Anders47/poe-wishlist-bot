package com.anders.poewishlist;

import com.anders.poewishlist.commands.PingCommand;
import com.anders.poewishlist.commands.SyncWishlistCommand;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bot {
    // local logging
    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    public static void main(String[] args) throws Exception {
        // Load token from env var
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("DISCORD_TOKEN not set – aborting startup.");
            return;
        }

        // Build JDA instance with MESSAGE_CONTENT intent
        JDABuilder builder = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                // Register all event listeners (commands, poller, etc.)
                .addEventListeners(
                        new PingCommand(),
                        new SyncWishlistCommand(
                                new com.anders.poewishlist.db.InMemoryWishlistStore()
                        )
                );

        // Start the bot
        builder.build();
        System.out.println("Bot is up and running!");
    }
}