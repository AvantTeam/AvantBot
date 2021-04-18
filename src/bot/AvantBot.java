package bot;

import bot.content.minigames.*;
import bot.core.*;

import org.slf4j.*;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.security.auth.login.*;

public class AvantBot {
    private static final Logger LOG = LoggerFactory.getLogger(AvantBot.class);

    public static final File ROOT_DIR;
    public static final File CLASSES_DIR;

    public static JDA jda;

    public static Settings settings;
    public static Commands commands;
    public static Warns warns;

    public static Messages messages;

    private static User creator;

    public static TicTacToe tictactoe;

    static {
        File home = Paths.get(".").toFile();
        if(
            List.of(home.list()).contains("resources") &&
            new File(home.getAbsolutePath(), "resources").isDirectory()
        ) {
            ROOT_DIR = new File(home.getAbsolutePath(), "resources");
        } else {
            ROOT_DIR = home;
        }

        System.setProperty("user.home", ROOT_DIR.getAbsolutePath());
        CLASSES_DIR = new File(ROOT_DIR.getAbsolutePath(), "classes" + File.separator);
    }

    public static void main(String[] args) {
        String token = System.getProperty("bot.token");
        if(token == null) {
            throw new IllegalStateException("Property 'bot.token' not found.");
        }

        LOG.info("Found bot token.");
        try {
            ROOT_DIR.mkdirs();
            CLASSES_DIR.mkdirs();
            LOG.debug("Root dir: '{}', classes dir: '{}'.", ROOT_DIR.getAbsolutePath(), CLASSES_DIR.getAbsolutePath());

            settings = new Settings();
            commands = new Commands();
            warns = new Warns();

            jda = JDABuilder
                .create(Set.of(GatewayIntent.values()))
                .setToken(token)
                .addEventListeners(
                    messages = new Messages()
                )
                .setBulkDeleteSplittingEnabled(false)
                .build()
                .awaitReady();

            String id = System.getProperty("bot.creator");
            if(id != null) {
                LOG.debug("Creator ID: {}.", id);
                creator = getUser(id);
                if(creator != null) {
                    LOG.info("Found creator: '{}#{}'.", creator.getName(), creator.getDiscriminator());
                } else {
                    LOG.debug("Bot creator not found; double check the user ID.");
                }
            }

            tictactoe = new TicTacToe();

            String last = (String)settings.remove("restart-message");
            if(last != null) {
                String[] split = last.split("-");
                jda.getGuildById(split[0])
                    .getTextChannelById(split[1])
                    .retrieveMessageById(split[2])
                    .flatMap(msg -> msg.reply("Successfully restarted."))
                    .queue();
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

    public static Member getOwner(Guild guild) {
        try {
            return guild.retrieveOwner(true).complete();
        } catch(Exception e) {
            return null;
        }
    }

    public static void exit(int code) {
        LOG.info("Shutting the JDA instance down.");
        settings.save();

        System.exit(code);
    }
}
