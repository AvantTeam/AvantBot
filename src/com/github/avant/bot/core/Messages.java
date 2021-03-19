package com.github.avant.bot.core;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.*;

import static com.github.avant.bot.AvantBot.*;

public class Messages extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        if(msg.getAuthor().isBot()) return;
        
        commands.handle(msg, event.getMember());
    }
}
