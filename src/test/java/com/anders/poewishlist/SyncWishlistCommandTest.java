package com.anders.poewishlist;

import com.anders.poewishlist.commands.SyncWishlistCommand;
import com.anders.poewishlist.db.InMemoryWishlistStore;
import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.service.WishlistParser;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SyncWishlistCommandTest {
    private static final String USER_ID = "test-user-123";

    private WishlistStore store;
    private WishlistParser parser;
    private SyncWishlistCommand command;

    private TextChannel channel;
    private RestAction<List<Message>> historyAction;

    @BeforeEach
    void setUp() {
        // 1) in-memory store + mocked parser
        store  = new InMemoryWishlistStore();
        parser = mock(WishlistParser.class);

        // 2) mock channel → history → restAction
        channel       = mock(TextChannel.class);
        MessageHistory history = mock(MessageHistory.class);
        historyAction = mock(RestAction.class);

        when(channel.getHistory()).thenReturn(history);
        when(history.retrievePast(100)).thenReturn(historyAction);

        // 3) command under test
        command = new SyncWishlistCommand(store, parser);
    }

    @Test
    void whenSync_thenStoreReceivesParsedResults() {
        // Given: parser returns exactly two items
        List<String> parsed = List.of("Mageblood", "Goldrim");
        when(historyAction.submit())
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(parser.parse(anyList(), eq(USER_ID)))
                .thenReturn(parsed);

        // When: we invoke sync
        command.sync(channel, USER_ID).join();

        // Then: store contains exactly the two parser items
        assertThat(store.getWishes(USER_ID), contains("Mageblood", "Goldrim"));
    }

    @Test
    void whenSync_thenClearUserCalledBeforeAdding() {
        // Given: some pre-existing wish for USER_ID
        store.addWish(USER_ID, "OldUnique");
        when(historyAction.submit())
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(parser.parse(anyList(), eq(USER_ID)))
                .thenReturn(List.of("NewUnique"));

        // When
        command.sync(channel, USER_ID).join();

        // Then: old wishes are gone, new one is present
        assertThat(store.getWishes(USER_ID), contains("NewUnique"));
    }

    @Test
    void whenSync_thenParserReceivesExactlyHistoryMessagesAndUserId() {
        // Given: two fake JDA Messages
        Message m1 = mock(Message.class);
        Message m2 = mock(Message.class);
        CompletableFuture<List<Message>> fut =
                CompletableFuture.completedFuture(List.of(m1, m2));
        when(historyAction.submit()).thenReturn(fut);

        // stub parser to return nothing (we just want to capture args)
        when(parser.parse(anyList(), eq(USER_ID))).thenReturn(List.of());

        // When
        command.sync(channel, USER_ID).join();

        // Then: parser.parse(...) was invoked with exactly [m1, m2], USER_ID
        verify(parser).parse(List.of(m1, m2), USER_ID);
    }
}