package com.github.avant.bot.core;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.*;

import static com.github.avant.bot.AvantBot.*;

public class Messages extends ListenerAdapter {
    private final String[] warns = { "once", "twice", "thrice", "four times", "too many times" };

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        if(msg.getAuthor().isBot()) return;
        
        commands.handle(msg, event.getMember());
    }

    public String warnMessage(int i) {
        return warns[Math.min(Math.max(i - 1, 0), warns.length - 1)];
    }

    public Member parseMention(String mention) {
        String strip = mention.substring(2, mention.length() - 1);
        if(strip.startsWith("!")) strip = strip.substring(1);

        try {
            return guild.retrieveMemberById(strip, true).complete();
        } catch(NumberFormatException e) {
            return null;
        }
    }

    public Role parseRole(String role) {
        String strip = role.substring(2, role.length() - 1);
        if(strip.startsWith("&")) strip = strip.substring(1);

        try {
            return guild.getRoleById(role);
        } catch(NumberFormatException e) {
            return null;
        }
    }
}
