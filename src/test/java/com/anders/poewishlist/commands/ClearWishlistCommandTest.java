package com.anders.poewishlist.commands;

import com.anders.poewishlist.db.InMemoryWishlistStore;
import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ClearWishlistCommandTest {

    private static final String USER_ID = "user-999";

    private WishlistStore store;
    private UniqueItemMatcher matcher;
    private ClearWishlistCommand cmd;

    private MessageReceivedEvent event;
    private MessageChannel channel;
    private Message msg;

    @BeforeEach
    void setUp() {
        // 1) Mock the incoming JDA event
        event = mock(MessageReceivedEvent.class);

        // 2) Stub the channel: one mock that is both the union and the MessageChannel
        MessageChannelUnion unionChannel = mock(
                MessageChannelUnion.class,
                withSettings().extraInterfaces(MessageChannel.class)
        );
        channel = (MessageChannel) unionChannel;
        // use doReturn because getChannel() is final
        doReturn(unionChannel).when(event).getChannel();

        // 3) Stub the user/author
        User user = mock(User.class);
        when(event.getAuthor()).thenReturn(user);
        when(user.isBot()).thenReturn(false);
        when(user.getId()).thenReturn(USER_ID);

        // 4) Stub the incoming Message
        msg = mock(Message.class);
        when(event.getMessage()).thenReturn(msg);

        // 5) Prepare store & matcher & command-under-test
        store   = new InMemoryWishlistStore();
        matcher = mock(UniqueItemMatcher.class);
        cmd = new ClearWishlistCommand(store);
    }

    @Test
    void givenNonEmptyWishlist_whenClear_thenStoreIsEmptiedAndConfirmed() {
        // seed the store
        store.addWish(USER_ID, "Headhunter");
        store.addWish(USER_ID, "Goldrim");

        // stub the incoming message to "!clearwishlist"
        when(msg.getContentRaw()).thenReturn("!clearwishlist");

        // stub sendMessage so .queue() won’t NPE
        MessageCreateAction action = mock(MessageCreateAction.class);
        when(channel.sendMessage("✅ Cleared your wishlist.")).thenReturn(action);

        // invoke
        cmd.onMessageReceived(event);

        // assert store is now empty
        assertThat(store.getWishes(USER_ID), is(empty()));

        // assert confirmation was sent
        verify(channel).sendMessage("✅ Cleared your wishlist.");
        verify(action).queue();
    }

}
