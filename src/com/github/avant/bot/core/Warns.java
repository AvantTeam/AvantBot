package com.github.avant.bot.core;

import net.dv8tion.jda.api.entities.*;

import java.util.*;

import static com.github.avant.bot.AvantBot.*;

@SuppressWarnings("unchecked")
public class Warns {
    public void warn(Message message, Member member, String reason) {
        Guild guild = member.getGuild();

        Map<String, Map<String, Map<String, Object>>> warns = settings.get("warns", Map.class);
        if(!warns.containsKey(guild.getId())) {
            warns.put(guild.getId(), new LinkedHashMap<>());
        }

        Map<String, Map<String, Object>> guildMap = warns.get(guild.getId());
        if(!guildMap.containsKey(member.getId())) {
            guildMap.put(member.getId(), new LinkedHashMap<>());
        }

        Map<String, Object> map = guildMap.get(member.getId());

        var count = (int)map.getOrDefault("count", 0) + 1;
        map.put("count", count);

        var reasons = (List<String>)map.getOrDefault("reasons", new ArrayList<String>());
        reasons.add(reason == null ? "Unspecified" : reason);
        map.put("reasons", reasons);

        settings.save();

        if(message != null) {
            message.getTextChannel()
                .sendMessage(String.format("%s, you have been warned **%s**%s",
                    member.getAsMention(),
                    messages.warnMessage(count),
                    reason == null
                    ?   "."
                    :   String.format(" with a reason: **%s**", reason)
                ))
                .queue();
        }
    }
}