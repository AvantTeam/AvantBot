package com.github.avant.bot;

import com.github.avant.bot.core.*;

import org.slf4j.*;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.*;

import java.util.*;
import javax.security.auth.login.*;

public class AvantBot {
    private static final Logger LOG = LoggerFactory.getLogger(AvantBot.class);

    public static JDA jda;

    public static Settings settings;
    public static Commands commands;
    public static Warns warns;

    public static Messages messages;

    private static User creator;

    public static void main(String[] args) {
        String token = System.getProperty("bot.token");
        if(token == null) {
            throw new IllegalStateException("Property 'bot.token' not found.");
        }

        LOG.debug("Found bot token.");
        try {
            settings = new Settings();
            commands = new Commands();
            warns = new Warns();

            jda = JDABuilder
                .create(Set.of(GatewayIntent.values()))
                .addEventListeners(
                    messages = new Messages()
                )
                .setToken(token)
                .build()
                .awaitReady();

            String id = System.getProperty("bot.creator");
            if(id != null) {
                LOG.debug("Creator ID: {}.", id);
                creator = getUser(id);
                if(creator != null) {
                    LOG.debug("Found creator: '{}#{}'.", creator.getName(), creator.getDiscriminator());
                } else {
                    LOG.debug("Bot creator not found; double check the user ID.");
                }
            }
        } catch(LoginException e) {
            throw new RuntimeException("Bot failed to log in", e);
        } catch(InterruptedException e) {
            throw new RuntimeException("Bot interrupted when logging in", e);
        }
    }

    public static String prefix() {
        return settings.get("prefix", String.class);
    }

    public static User creator() {
        return creator;
    }

    public static Member self(Guild guild) {
        return guild.retrieveMember(jda.getSelfUser()).complete();
    }

    public static User self() {
        return jda.getSelfUser();
    }

    public static User getUser(String id) {
        try {
            return jda.retrieveUserById(id, true).complete();
        } catch(Exception e) {
            return null;
        }
    }

    public static Member getMember(Guild guild, User user) {
        try {
            return guild.retrieveMember(user, true).complete();
        } catch(Exception e) {
            return null;
        }
    }

    public static Role getRole(Guild guild, String id) {
        try {
            return guild.getRoleById(id);
        } catch(Exception e) {
            return null;
        }
    }
}
