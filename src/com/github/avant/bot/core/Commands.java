package com.github.avant.bot.core;

import com.github.avant.bot.content.*;

import org.slf4j.*;

import net.dv8tion.jda.api.entities.*;

import java.util.*;
import java.util.regex.*;

import static com.github.avant.bot.AvantBot.*;

public class Commands {
    private static final Logger LOG = LoggerFactory.getLogger(Commands.class);

    private final Pattern splitter = Pattern.compile("(?:```(?:.|[\\s\\S])*```)|(?:\".+\")|(?:[^\\s]+)");

    public Commands() {
        LOG.debug("Initialized command handler.");
    }

    public void handle(Message message, Member member) {
        String text = message.getContentRaw();
        String cont;
        String pref = prefix();

        if(
            !text.startsWith(pref) ||
            (cont = text.substring(pref.length())).isBlank()
        ) {
            return;
        }

        List<String> split = new ArrayList<>();

        Matcher matcher = splitter.matcher(cont);
        while(matcher.find()) {
            String group = matcher.group();
            if(group.startsWith("\"") && group.endsWith("\"")) {
                group = group.substring(1, group.length() - 1);
            }

            if(group.startsWith("```") && group.endsWith("```")) {
                group = group.replaceAll("```.*", "");
            }

            split.add(group);
        }

        String name = split.remove(0);
        Command command;
        if((command = messages.commandExists(message, name)) != null) {
            int size = command.minArgSize();
            if(split.size() < size) {
                message.getTextChannel()
                    .sendMessage(String.format("Insufficient amount or arguments *(supplied: %d, required: %d)*.", split.size(), size))
                    .queue();
            } else {
                command.execute(message, split);
            }
        }
    }
}
