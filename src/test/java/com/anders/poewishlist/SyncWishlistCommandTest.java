package com.anders.poewishlist;

import com.anders.poewishlist.commands.SyncWishlistCommand;
import com.anders.poewishlist.db.InMemoryWishlistStore;
import com.anders.poewishlist.db.WishlistStore;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

class SyncWishlistCommandTest {

    private static final String USER_ID = "test-user-123";

    private WishlistStore store;
    private SyncWishlistCommand command;
    private TextChannel channel;
    private MessageHistory history;
    private RestAction<List<Message>> fakeHistoryAction;

    @BeforeEach
    void setUp() {
        // Given a fresh in-memory store and a mocked TextChannel
        store   = new InMemoryWishlistStore();
        channel = mock(TextChannel.class);
        when(channel.getId()).thenReturn(USER_ID);

        // Mock MessageHistory and stub getHistory()
        history = mock(MessageHistory.class);
        when(channel.getHistory()).thenReturn(history);

        // Prepare a fake RestAction for retrievePast(...)
        @SuppressWarnings("unchecked")
        RestAction<List<Message>> action = mock(RestAction.class);
        fakeHistoryAction = action;
        when(history.retrievePast(100)).thenReturn(fakeHistoryAction);

        // Command uses this store
        command = new SyncWishlistCommand(store);
    }

    @Test
    void givenThreeLinesInChannelHistory_whenSync_thenStoreHasExactlyThoseThreeWishes() {
        // Given: three mock messages with wishlist lines
        Message wish1 = mock(Message.class);
        Message wish2 = mock(Message.class);
        Message wish3 = mock(Message.class);
        when(wish1.getContentRaw()).thenReturn("Mageblood");
        when(wish2.getContentRaw()).thenReturn("Goldrim");
        when(wish3.getContentRaw()).thenReturn("Wanderlust");

        // When: submit() is called on the history action, return a completed future
        List<Message> mockMessages = List.of(wish1, wish2, wish3);
        CompletableFuture<List<Message>> future = CompletableFuture.completedFuture(mockMessages);
        when(fakeHistoryAction.submit()).thenReturn(future);

        // When: we call sync and wait for it to finish
        command.sync(channel).join();

        // Then: the store contains exactly those three items, in the same order
        List<String> wishes = store.getWishes(USER_ID);
        assertThat("Should have 3 wishes", wishes.size(), is(3));
        assertThat("First wish",  wishes.get(0), is("Mageblood"));
        assertThat("Second wish", wishes.get(1), is("Goldrim"));
        assertThat("Third wish",  wishes.get(2), is("Wanderlust"));
    }
}