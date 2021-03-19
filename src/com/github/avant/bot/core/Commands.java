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

        Command command = Command.valueOf(split.remove(0).toUpperCase());
        if(command.permission.qualified(member)) {
            command.execute(message, split);
        }
    }
}
