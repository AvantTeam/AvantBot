package com.github.avant.bot.core;

import com.github.avant.bot.content.*;

import net.dv8tion.jda.api.entities.*;

import java.util.*;

import static com.github.avant.bot.AvantBot.*;

public class Commands {
    public void handle(Message message, Member member) {
        String text = message.getContentStripped();
        String cont;
        String pref = prefix();

        if(
            !text.startsWith(pref) ||
            (cont = text.substring(pref.length())).isBlank()
        ) {
            return;
        }

        List<String> split = new ArrayList<>();
        for(String c : cont.split("\\s+")) split.add(c);

        Command command = Command.valueOf(split.remove(0).toUpperCase());

        if(command.permission.qualified(member)) {
            command.execute(message, split);
        }
    }
}
