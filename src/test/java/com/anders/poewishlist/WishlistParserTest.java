package com.anders.poewishlist;

import com.anders.poewishlist.service.WishlistParser;
import com.anders.poewishlist.util.UniqueItemMatcher;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

class WishlistParserTest {
    private static final String USER_ID = "user-A";

    private UniqueItemMatcher matcher;
    private WishlistParser parser;
    private Message msg;

    @BeforeEach
    void setUp() {
        // prepare a parser with a mocked matcher
        matcher = mock(UniqueItemMatcher.class);
        parser  = new WishlistParser(matcher);
        msg     = mock(Message.class);

        // default author=A for each message
        User authorA = mock(User.class);
        when(authorA.getId()).thenReturn(USER_ID);
        when(msg.getAuthor()).thenReturn(authorA);
    }

    @Test
    void givenCommandAndConfirmation_whenParse_thenOnlyValidUniques() {
        // Given: one message mixing a command, a real unique and a confirmation
        when(msg.getContentRaw()).thenReturn(
                "!syncwishlist\n" +
                        "Wanderlust\n" +
                        "✅ Wishlist synchronized!"
        );
        // stub matcher: only "Wanderlust" should match
        when(matcher.match("Wanderlust")).thenReturn("Wanderlust");

        // When
        List<String> result = parser.parse(List.of(msg), USER_ID);

        // Then
        assertThat(result, contains("Wanderlust"));
    }

    @Test
    void givenMultiLineMessage_whenParse_thenSplitIntoSeparateUniques() {
        // Given: a single message with three uniques separated by newline
        when(msg.getContentRaw()).thenReturn(
                "Hrimsorrow\nGoldrim\nHeadhunter"
        );
        // stub matcher for each line
        when(matcher.match("Hrimsorrow")).thenReturn("Hrimsorrow");
        when(matcher.match("Goldrim")).thenReturn("Goldrim");
        when(matcher.match("Headhunter")).thenReturn("Headhunter");

        // When
        List<String> result = parser.parse(List.of(msg), USER_ID);

        // Then
        assertThat(result, contains("Hrimsorrow", "Goldrim", "Headhunter"));
    }

    @Test
    void givenCommaSeparated_whenParse_thenSplitOnComma() {
        // Given: one message with three uniques separated by commas
        when(msg.getContentRaw()).thenReturn(
                "Headhunter, Goldrim, Wanderlust"
        );
        when(matcher.match("Headhunter")).thenReturn("Headhunter");
        when(matcher.match("Goldrim")).thenReturn("Goldrim");
        when(matcher.match("Wanderlust")).thenReturn("Wanderlust");

        // When
        List<String> result = parser.parse(List.of(msg), USER_ID);

        // Then
        assertThat(result, contains("Headhunter", "Goldrim", "Wanderlust"));
    }

    @Test
    void givenTypoAndMixedCase_whenParse_thenCorrectToCanonical() {
        // Given: one typo’ed unique
        when(msg.getContentRaw()).thenReturn("heaDhunter");
        // stub matcher to fix typo
        when(matcher.match("heaDhunter")).thenReturn("Headhunter");

        // When
        List<String> result = parser.parse(List.of(msg), USER_ID);

        // Then
        assertThat(result, contains("Headhunter"));
    }

    @Test
    void givenMessageFromOtherUser_whenParse_thenReturnEmpty() {
        // Given: author != USER_ID
        User authorB = mock(User.class);
        when(authorB.getId()).thenReturn("user-B");
        when(msg.getAuthor()).thenReturn(authorB);
        when(msg.getContentRaw()).thenReturn("Headhunter");

        // When
        List<String> result = parser.parse(List.of(msg), USER_ID);

        // Then
        assertThat(result, is(empty()));
    }


    @Test
    void givenNoMatchesFromMatcher_whenParse_thenReturnEmptyList() {
        when(msg.getContentRaw()).thenReturn("UnknownItem");
        when(matcher.match("UnknownItem")).thenReturn(null);

        List<String> result = parser.parse(List.of(msg), USER_ID);

        assertThat(result, is(empty()));
    }

    @Test
    void givenMultipleMessages_whenParse_thenMergeAllItems() {
        // first message with two items
        Message m1 = mock(Message.class);
        User authorA = mock(User.class);
        when(authorA.getId()).thenReturn(USER_ID);
        when(m1.getAuthor()).thenReturn(authorA);
        when(m1.getContentRaw()).thenReturn("Headhunter\nGoldrim");
        when(matcher.match("Headhunter")).thenReturn("Headhunter");
        when(matcher.match("Goldrim")).thenReturn("Goldrim");

        // second message with one item
        Message m2 = mock(Message.class);
        when(m2.getAuthor()).thenReturn(authorA);
        when(m2.getContentRaw()).thenReturn("Wanderlust");
        when(matcher.match("Wanderlust")).thenReturn("Wanderlust");

        List<String> result = parser.parse(List.of(m1, m2), USER_ID);

        assertThat(result, contains("Headhunter", "Goldrim", "Wanderlust"));
    }


    @Test
    void givenDuplicateEntriesAcrossMessages_whenParse_thenRemoveDuplicates() {
        // two messages both containing "Wanderlust"
        Message m1 = mock(Message.class);
        Message m2 = mock(Message.class);
        User authorA = mock(User.class);
        when(authorA.getId()).thenReturn(USER_ID);
        when(m1.getAuthor()).thenReturn(authorA);
        when(m2.getAuthor()).thenReturn(authorA);

        when(m1.getContentRaw()).thenReturn("Wanderlust");
        when(m2.getContentRaw()).thenReturn("Wanderlust");
        when(matcher.match("Wanderlust")).thenReturn("Wanderlust");

        List<String> result = parser.parse(List.of(m1, m2), USER_ID);

        assertThat(result, contains("Wanderlust"));
    }

    @Test
    void givenBlankAndWhitespaceLines_whenParse_thenIgnoreEmptyParts() {
        when(msg.getContentRaw()).thenReturn(
                "  \nHeadhunter  \n   \nGoldrim\n"
        );
        when(matcher.match("Headhunter")).thenReturn("Headhunter");
        when(matcher.match("Goldrim")).thenReturn("Goldrim");

        List<String> result = parser.parse(List.of(msg), USER_ID);

        assertThat(result, contains("Headhunter", "Goldrim"));
    }
}