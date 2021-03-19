package com.github.avant.bot.core;

import com.github.avant.bot.content.*;

import net.dv8tion.jda.api.entities.*;

import java.util.*;
import java.util.regex.*;

import static com.github.avant.bot.AvantBot.*;

public class Commands {
    private Pattern splitter = Pattern.compile("(?:\".+\")|(?:[^\\s]+)");

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
            split.add(matcher.group().replace("\"", ""));
        }

        String name = split.remove(0);
        Command command;

        try {
            command = Command.forName(name);
        } catch(Exception e) {
            command = null;
        }

        if(command == null) {
            message.getTextChannel().sendMessage("Unknown command: '" + name + "'.");
        } else if(command.permission.qualified(member)) {
            command.execute(message, split);
        }
    }
}
