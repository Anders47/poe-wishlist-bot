package com.anders.poewishlist.service;

import com.anders.poewishlist.api.StashApiClient;
import com.anders.poewishlist.api.StashResponse;
import com.anders.poewishlist.db.CursorStore;
import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service that periodically scans the PoE stash API for new stash entries,
 * matches items against user wishlists, and sends Discord notifications.
 * Supports exponential back-off on rate-limit (HTTP 429) errors.
 */
public class ScannerService {
    private static final Logger logger = LoggerFactory.getLogger(ScannerService.class);
    private static final int INITIAL_BACKOFF_MS = 5000;
    private static final int MAX_BACKOFF_MS = 60000;
    private static final int MAX_RETRIES = 5;

    private final StashApiClient apiClient;
    private final CursorStore cursorStore;
    private final WishlistStore wishlistStore;
    private final UniqueItemMatcher matcher;
    private final JDA jda;
    private final String channelId;
    private final String leagueName;
    private final int intervalMinutes;
    private final ScheduledExecutorService scheduler;
    private final Sleeper sleeper;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Constructs a ScannerService.
     *
     * @param apiClient       the stash API client
     * @param cursorStore     store for persisting next_change_id
     * @param wishlistStore   store for user wishlists
     * @param matcher         utility for matching unique item names
     * @param jda             JDA instance for Discord communication
     * @param channelId       Discord channel ID for notifications
     * @param leagueName      PoE league to monitor
     * @param intervalMinutes polling interval in minutes
     * @param scheduler       scheduler for periodic execution
     * @param sleeper         utility for sleeping between retries
     */
    public ScannerService(
            StashApiClient apiClient,
            CursorStore cursorStore,
            WishlistStore wishlistStore,
            UniqueItemMatcher matcher,
            JDA jda,
            String channelId,
            String leagueName,
            int intervalMinutes,
            ScheduledExecutorService scheduler,
            Sleeper sleeper
    ) {
        this.apiClient = apiClient;
        this.cursorStore = cursorStore;
        this.wishlistStore = wishlistStore;
        this.matcher = matcher;
        this.jda = jda;
        this.channelId = channelId;
        this.leagueName = leagueName;
        this.intervalMinutes = intervalMinutes;
        this.scheduler = scheduler;
        this.sleeper = sleeper;
    }

    /**
     * Start the periodic scan task.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this::scanWithRetry, 0, intervalMinutes, TimeUnit.MINUTES);
    }

    /**
     * Stop the scan task immediately.
     */
    public void stop() {
        scheduler.shutdownNow();
    }

    // Wrap scan in retry logic
    void scanWithRetry() {
        int attempts = 0;
        int backoff = INITIAL_BACKOFF_MS;
        while (true) {
            try {
                scanTask();
                return;
            } catch (Exception e) {
                int code = (e instanceof com.anders.poewishlist.api.ApiException)
                        ? ((com.anders.poewishlist.api.ApiException) e).getStatusCode()
                        : -1;
                if (code == 429 && attempts < MAX_RETRIES) {
                    logger.warn("Rate limited (429). Backoff {} ms before retry {}", backoff, attempts + 1);
                    try { sleeper.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
                    attempts++;
                } else {
                    logger.error("Scan task failed", e);
                    return;
                }
            }
        }
    }

    // Single iteration of fetch->filter->notify->persist
    private void scanTask() throws Exception {
        String cursor = cursorStore.getCursor();
        StashResponse response = apiClient.fetch(leagueName, cursor);
        String newCursor = response.getNextChangeId();

        response.getStashes().forEach(tab ->
                tab.getItems().stream()
                        .map(item -> matcher.match(item.getName()))
                        .filter(Objects::nonNull)
                        .forEach(this::notifyUsers)
        );

        cursorStore.setCursor(newCursor);
    }

    // Notify all users who have this unique on their wishlist
    private void notifyUsers(String uniqueName) {
        List<String> userIds = wishlistStore.getUsersWithItem(uniqueName);
        String timestamp = LocalDateTime.now().format(timeFmt);
        userIds.forEach(userId -> {
            String message = String.format("<@%s> has found %s on your list at %s!", userId, uniqueName, timestamp);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(message).queue();
            }
        });
    }

    /**
     * Sleeper abstraction for injecting sleep behavior.
     */
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    /**
     * Default Sleeper using Thread.sleep.
     */
    public static class ThreadSleeper implements Sleeper {
        @Override public void sleep(long millis) throws InterruptedException { Thread.sleep(millis); }
    }
}