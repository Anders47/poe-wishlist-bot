package com.anders.poewishlist.commands;

import com.anders.poewishlist.db.InMemoryWishlistStore;
import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.service.WishlistParser;
import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class RemoveFromWishlistCommandTest {
    private static final String USER_ID = "user-999";

    private WishlistStore store;
    private UniqueItemMatcher matcher;
    private RemoveFromWishlistCommand cmd;
    private WishlistParser parser;
    private MessageReceivedEvent event;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        event = mock(MessageReceivedEvent.class);

        // 1) Create one mock that is both union & channel:
        MessageChannelUnion unionChannel = mock(
                MessageChannelUnion.class,
                withSettings().extraInterfaces(MessageChannel.class)
        );
        // assign it to your test‐level field
        this.channel = unionChannel;

        // 2) Stub the final getChannel() via doReturn
        doReturn(unionChannel).when(event).getChannel();

        // author stub
        User user = mock(User.class);
        when(event.getAuthor()).thenReturn(user);
        when(user.isBot()).thenReturn(false);
        when(user.getId()).thenReturn(USER_ID);

        // prepare a message stub
        Message msg = mock(Message.class);
        when(event.getMessage()).thenReturn(msg);
        when(msg.getContentRaw()).thenReturn("!remove heaDhunter");

        // real store + seed it
        store   = new InMemoryWishlistStore();
        store.addWish(USER_ID, "Headhunter");
        matcher = mock(UniqueItemMatcher.class);
        parser = new WishlistParser(matcher);
        cmd     = new RemoveFromWishlistCommand(store, matcher, parser);
    }

    @Test
    void givenExistingUnique_whenRemove_thenItIsGoneAndConfirmed() {
        // matcher will canonicalize the typo
        when(matcher.match("heaDhunter")).thenReturn("Headhunter");
        // stub sendMessage so .queue() won’t NPE
        var action = mock(net.dv8tion.jda.api.requests.restaction.MessageCreateAction.class);
        when(channel.sendMessage("✅ Removed **Headhunter** from your wishlist."))
                .thenReturn(action);

        // act
        cmd.onMessageReceived(event);

        // assert removal
        assertThat(store.getWishes(USER_ID), is(empty()));
        // assert confirmation
        verify(channel).sendMessage("✅ Removed **Headhunter** from your wishlist.");
        verify(action).queue();
    }
}
