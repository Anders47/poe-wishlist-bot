package com.anders.poewishlist.service;

import com.anders.poewishlist.api.ApiException;
import com.anders.poewishlist.api.StashApiClient;
import com.anders.poewishlist.api.StashResponse;
import com.anders.poewishlist.db.CursorStore;
import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScannerServiceTest {
    private StashApiClient apiClient;
    private CursorStore cursorStore;
    private WishlistStore wishlistStore;
    private UniqueItemMatcher matcher;
    private JDA jda;
    private TextChannel channel;
    private ScheduledExecutorService scheduler;
    private ScannerService.Sleeper sleeper;
    private ScannerService service;

    @BeforeEach
    void setup() {
        apiClient = mock(StashApiClient.class);
        cursorStore = mock(CursorStore.class);
        wishlistStore = mock(WishlistStore.class);
        matcher = mock(UniqueItemMatcher.class);
        jda = mock(JDA.class);
        channel = mock(TextChannel.class);
        scheduler = mock(ScheduledExecutorService.class);
        sleeper = mock(ScannerService.Sleeper.class);

        when(jda.getTextChannelById(any())).thenReturn(channel);
        service = new ScannerService(apiClient, cursorStore, wishlistStore, matcher,
                jda, "chanId", "league", 5, scheduler, sleeper);
    }

    @Test
    void startSchedulesTaskAtFixedRate() {
        service.start();
        verify(scheduler).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void scanWithRetryRetriesOn429() throws Exception {
        // Simulate 429 first two times, then success
        ApiException rateLimit = new ApiException(429, "Too many requests");
        StashResponse okResp = mock(StashResponse.class);
        when(cursorStore.getCursor()).thenReturn(null);
        when(apiClient.fetch("league", null))
                .thenThrow(rateLimit)
                .thenThrow(rateLimit)
                .thenReturn(okResp);
        when(okResp.getNextChangeId()).thenReturn("newId");
        when(okResp.getStashes()).thenReturn(Collections.emptyList());

        service.scanWithRetry();

        InOrder inOrder = inOrder(apiClient, sleeper, cursorStore);
        // First attempt
        inOrder.verify(apiClient, times(1)).fetch("league", null);
        inOrder.verify(sleeper).sleep(anyLong());
        // Second retry
        inOrder.verify(apiClient, times(1)).fetch("league", null);
        inOrder.verify(sleeper).sleep(anyLong());
        // Third attempt succeeds
        inOrder.verify(apiClient, times(1)).fetch("league", null);
        inOrder.verify(cursorStore).setCursor("newId");
    }

    @Test
    void scanWithRetryStopsAfterNon429() throws Exception {
        ApiException otherError = new ApiException(500, "Server error");
        when(cursorStore.getCursor()).thenReturn(null);
        when(apiClient.fetch("league", null)).thenThrow(otherError);

        service.scanWithRetry();

        verify(apiClient).fetch("league", null);
        verifyNoInteractions(sleeper);
    }
}