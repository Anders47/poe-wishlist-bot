package com.anders.poewishlist.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import com.anders.poewishlist.db.InMemoryWishlistStore;
import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.service.WishlistParser;
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

class AddToWishlistCommandTest {
    private static final String USER_ID = "user-123";
    private WishlistStore store;
    private WishlistParser parser;
    private UniqueItemMatcher matcher;
    private AddToWishlistCommand cmd;
    private MessageReceivedEvent event;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        event = mock(MessageReceivedEvent.class);

        MessageChannelUnion unionChannel = mock(
                MessageChannelUnion.class,
                withSettings().extraInterfaces(MessageChannel.class)
        );
        this.channel = unionChannel;


        // Stub getChannel() once, to return the union+channel mock:
        doReturn(unionChannel).when(event).getChannel();

        // stub the user
        User user = mock(User.class);
        when(event.getAuthor()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        when(user.isBot()).thenReturn(false);

        // stub the incoming message
        Message msg = mock(Message.class);
        when(event.getMessage()).thenReturn(msg);
        when(msg.getContentRaw()).thenReturn("!add heaDhunter");

        store = new InMemoryWishlistStore();
        matcher = mock(UniqueItemMatcher.class);
        parser = new WishlistParser(matcher);
        cmd = new AddToWishlistCommand(store, matcher, parser);
    }

    @Test
    void givenValidUnique_whenAdd_thenStoreAndConfirm() {
        // stub matcher
        when(matcher.match("heaDhunter")).thenReturn("Headhunter");

        // stub sendMessage to return a mock action so .queue() won't NPE
        MessageCreateAction messageAction = mock(MessageCreateAction.class);
        when(channel.sendMessage("✅ Added **Headhunter** to your wishlist."))
                .thenReturn(messageAction);

        // invoke
        cmd.onMessageReceived(event);

        // assertions
        assertThat(store.getWishes(USER_ID), contains("Headhunter"));
        verify(channel).sendMessage("✅ Added **Headhunter** to your wishlist.");
        verify(messageAction).queue();
    }
}