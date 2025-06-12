package com.anders.poewishlist;

import com.anders.poewishlist.commands.ClearWishlistCommand;
import com.anders.poewishlist.commands.AddToWishlistCommand;
import com.anders.poewishlist.commands.ListWishlistCommand;
import com.anders.poewishlist.commands.PingCommand;
import com.anders.poewishlist.commands.RemoveFromWishlistCommand;
import com.anders.poewishlist.db.DatabaseWishlistStore;
import com.anders.poewishlist.db.CursorStore;
import com.anders.poewishlist.db.DatabaseCursorStore;
import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.service.ScannerService;
import com.anders.poewishlist.service.ScannerService.ThreadSleeper;
import com.anders.poewishlist.service.ScannerService.Sleeper;
import com.anders.poewishlist.service.WishlistParser;
import com.anders.poewishlist.util.UniqueItemMatcher;
import com.anders.poewishlist.api.HttpStashApiClient;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Bot {
    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    public static void main(String[] args) throws Exception {
        String token = System.getenv("DISCORD_TOKEN");
        String league = System.getenv("POE_LEAGUE");
        String channelId = System.getenv("CHANNEL_ID");
        int pollInterval = Integer.parseInt(System.getenv().getOrDefault("POLL_INTERVAL_MINUTES", "5"));

        if (token == null || token.isBlank() || league == null || league.isBlank() || channelId == null || channelId.isBlank()) {
            System.err.println("Required env vars DISCORD_TOKEN, POE_LEAGUE, CHANNEL_ID must be set – aborting startup.");
            return;
        }
        log.info("Starting PoE Wishlist Bot…");

        // Setup SQLite DataSource
        String jdbcUrl = "jdbc:sqlite:poewishlist.db";
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        DataSource ds = new HikariDataSource(config);

        // Initialize stores, parser, matcher
        WishlistStore store = new DatabaseWishlistStore(ds);
        CursorStore cursorStore = new DatabaseCursorStore(ds);
        UniqueItemMatcher matcher = new UniqueItemMatcher();
        WishlistParser parser = new WishlistParser(matcher);

        // Build JDA
        JDABuilder builder = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(
                        new PingCommand(),
                        new ListWishlistCommand(store),
                        new AddToWishlistCommand(store, matcher, parser),
                        new RemoveFromWishlistCommand(store, matcher, parser),
                        new ClearWishlistCommand(store)
                );

        JDA jda = builder.build();
        jda.awaitReady();
        log.info("Bot is up and running!");

        // Start scanner service
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Sleeper sleeper = new ThreadSleeper();
        ScannerService scanner = new ScannerService(
                new HttpStashApiClient(),
                cursorStore,
                store,
                matcher,
                jda,
                channelId,
                league,
                pollInterval,
                scheduler,
                sleeper
        );
        scanner.start();
        log.info("Scanner service started, polling every {} minutes.", pollInterval);
    }
}
