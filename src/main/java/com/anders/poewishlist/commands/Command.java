package com.anders.poewishlist.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Command {
    void handle(MessageReceivedEvent event);
    String trigger();
}