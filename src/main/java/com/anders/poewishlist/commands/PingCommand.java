package com.anders.poewishlist.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class PingCommand extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String msg = event.getMessage().getContentRaw().trim();
        if (msg.equalsIgnoreCase("!ping")) {
            event.getChannel()
                    .sendMessageFormat("Pong! 🏓 (latency: %d ms)",
                            event.getJDA().getGatewayPing())
                    .queue();
        }
    }
}