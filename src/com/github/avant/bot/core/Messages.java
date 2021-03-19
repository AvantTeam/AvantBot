package com.github.avant.bot.core;

import com.github.avant.bot.content.*;

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

    public Command commandExists(Message message, String name) {
        Command command = Command.forName(name);
        Member member = message.getMember();

        if(command == null || !command.permission.qualified(member)) {
            message.getTextChannel().sendMessage(String.format("%s, command '%s' does not exist or you do not have permission to use it.", member.getAsMention(), name));
            return null;
        } else {
            return command;
        }
    }

    public Member memberExists(Message message, String mention) {
        Member target = getMember(message.getGuild(), parseMention(mention));
        Member member = message.getMember();

        if(target == null) {
            message.getTextChannel().sendMessage(String.format("%s, '%s' does not seem to represent a server member.", member.getAsMention(), mention));
            return null;
        } else {
            return target;
        }
    }

    public String warnMessage(int i) {
        return warns[Math.min(Math.max(i - 1, 0), warns.length - 1)];
    }

    public User parseMention(String mention) {
        String strip = mention.substring(2, mention.length() - 1);
        if(strip.startsWith("!")) strip = strip.substring(1);

        return getUser(strip);
    }

    public Role parseRole(Guild guild, String role) {
        String strip = role.substring(2, role.length() - 1);
        if(strip.startsWith("&")) strip = strip.substring(1);

        return getRole(guild, strip);
    }
}
