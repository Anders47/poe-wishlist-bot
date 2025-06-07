package com.anders.poewishlist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BotTest {
    @Test
    void botStarterWithoutException() {
        // no throwing, or angy >:(
        assertDoesNotThrow(() -> Bot.main(new String[0]));
    }
}
