package com.github.avant.bot.core;

import com.github.avant.bot.content.*;

import org.slf4j.*;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.*;

import java.io.*;

import static com.github.avant.bot.AvantBot.*;

public class Messages extends ListenerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(Messages.class);

    private final String[] warns = { "once", "twice", "thrice", "four times", "too many times" };

    public Messages() {
        LOG.debug("Initialized message listener.");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        Member member = event.getMember();

        if(member.getUser().isBot() || msg.getChannel() instanceof PrivateChannel) {
            return;
        }

        LOG.debug("{}#{} in #{}: {}", member.getEffectiveName(), member.getUser().getDiscriminator(), event.getTextChannel().getName(), msg.getContentDisplay());

        try {
            commands.handle(msg, event.getMember());
        } catch(Throwable t) {
            LOG.error("An error occurred", t);

            if(member.getIdLong() == creator().getIdLong()) {
                member.getUser().openPrivateChannel()
                    .flatMap(channel -> {
                        StringWriter writer = new StringWriter();
                        PrintWriter print = new PrintWriter(writer);

                        t.printStackTrace(print);
                        return channel.sendMessage(String.format("An error occured: ```\n%s```", writer.toString()));
                    })
                    .queue();
            } else {
                event.getTextChannel()
                    .sendMessage("An error occured.")
                    .queue();
            }
        }
    }

    public Command commandExists(Message message, String name) {
        Command command = Command.forName(name);
        Member member = message.getMember();

        if(command == null || !command.permission.qualified(member)) {
            message.getTextChannel()
                .sendMessage(String.format("%s, command '%s' does not exist or you do not have permission to use it.", member.getAsMention(), name))
                .queue();

            return null;
        } else {
            return command;
        }
    }

    public Member memberExists(Message message, String mention) {
        Member target = getMember(message.getGuild(), parseMention(mention));
        Member member = message.getMember();

        if(target == null) {
            message.getTextChannel()
                .sendMessage(String.format("%s, '%s' does not seem to represent a server member.", member.getAsMention(), mention))
                .queue();

            return null;
        } else {
            return target;
        }
    }

    public String warnMessage(int i) {
        return warns[Math.min(Math.max(i - 1, 0), warns.length - 1)];
    }

    public User parseMention(String mention) {
        String strip = mention;
        if(strip.startsWith("<@") && strip.endsWith(">")) strip = strip.substring(2, mention.length() - 1);
        if(strip.startsWith("!")) strip = strip.substring(1);

        return getUser(strip);
    }

    public Role parseRole(Guild guild, String role) {
        String strip = role;
        if(strip.startsWith("<&") && strip.endsWith(">")) strip = strip.substring(2, role.length() - 1);
        if(strip.startsWith("!")) strip = strip.substring(1);

        return getRole(guild, strip);
    }
}
