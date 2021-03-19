package com.github.avant.bot;

import com.github.avant.bot.core.*;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.requests.*;

import java.util.*;
import javax.security.auth.login.*;

public class AvantBot {
    public static JDA jda;

    public static Settings settings;

    public static Messages messages;

    public static void main(String[] args) {
        String token = System.getProperty("bot.token");
        if(token == null) {
            throw new IllegalStateException("Property 'bot.token' not found.");
        }

        try {
            settings = new Settings();

            jda = JDABuilder
                .create(Set.of(GatewayIntent.values()))
                .addEventListeners(
                    messages = new Messages()
                )
                .setToken(token)
                .build()
                .awaitReady();
        } catch(LoginException e) {
            throw new RuntimeException("Bot failed to log in", e);
        } catch(InterruptedException e) {
            throw new RuntimeException("Bot interrupted when logging in", e);
        }
    }
}
