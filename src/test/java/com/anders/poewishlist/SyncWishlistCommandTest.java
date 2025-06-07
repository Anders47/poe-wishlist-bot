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

    @Test
    void givenCommandAndConfirmationMessages_whenSync_thenOnlyValidItemsAreStored() {
        // Given: a channel history mixing a command, a real item, and the bot's own confirmation
        Message cmdMsg  = mock(Message.class);
        Message itemMsg = mock(Message.class);
        Message botMsg  = mock(Message.class);
        when(cmdMsg.getContentRaw()).thenReturn("!syncwishlist");
        when(itemMsg.getContentRaw()).thenReturn("Wanderlust");
        when(botMsg.getContentRaw()).thenReturn("✅ Wishlist synchronized! Loaded 1 items.");

        // Mock history and submission
        when(channel.getHistory()).thenReturn(history);
        when(history.retrievePast(100)).thenReturn(fakeHistoryAction);
        when(fakeHistoryAction.submit())
                .thenReturn(CompletableFuture.completedFuture(List.of(cmdMsg, itemMsg, botMsg)));

        // When: we sync
        command.sync(channel).join();

        // Then: only the real item remains
        List<String> wishes = store.getWishes(USER_ID);
        assertThat("Should store exactly one valid wish", wishes.size(), is(1));
        assertThat("That wish must be the real item", wishes.get(0), is("Wanderlust"));
    }

    @Test
    void givenSingleMessageWithMultipleUniques_whenSync_thenStoreHasAllItems() {
        // Given: én besked med tre unikke items adskilt af newline
        Message multiLineMsg = mock(Message.class);
        String payload = String.join("\n",
                "Hrimsorrow",
                "Goldrim",
                "Headhunter"
        );
        when(multiLineMsg.getContentRaw()).thenReturn(payload);

        // history.returner kun denne ene besked
        when(history.retrievePast(100)).thenReturn(fakeHistoryAction);
        when(fakeHistoryAction.submit())
                .thenReturn(CompletableFuture.completedFuture(List.of(multiLineMsg)));

        // When: vi kalder sync
        command.sync(channel).join();

        // Then: alle tre items skal være gemt som separate wishes
        List<String> wishes = store.getWishes(USER_ID);
        assertThat("Should store exactly 3 wishes", wishes.size(), is(3));
        assertThat("First unique",  wishes.get(0), is("Hrimsorrow"));
        assertThat("Second unique", wishes.get(1), is("Goldrim"));
        assertThat("Third unique",  wishes.get(2), is("Headhunter"));
    }

    @Test
    void givenTypoInWishlist_whenSync_thenTypoIsCorrectedToCanonicalUnique() {
        // Given: one message with a typo’ed unique name
        Message typoMsg = mock(Message.class);
        when(typoMsg.getContentRaw()).thenReturn("heaDhunter");  // typo + mixed case

        when(history.retrievePast(100)).thenReturn(fakeHistoryAction);
        when(fakeHistoryAction.submit())
                .thenReturn(CompletableFuture.completedFuture(List.of(typoMsg)));

        // When: we run sync
        command.sync(channel).join();

        // Then: store should contain the canonical "Headhunter"
        List<String> wishes = store.getWishes(USER_ID);
        assertThat("Should correct typo to canonical name", wishes, is(List.of("Headhunter")));
    }
}