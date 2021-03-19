package com.github.avant.bot.content;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;

import java.util.*;

import static com.github.avant.bot.AvantBot.*;
import static com.github.avant.bot.content.Command.CommandPermission.*;

@SuppressWarnings("unchecked")
public enum Command {
    HELP("help", DEFAULT) {
        @Override
        public void execute(Message message, List<String> args) {
            
        }
    },
    
    WARN("warn", ADMIN_ONLY) {
        {
            params = List.of(
                new CommandParam(false, "user"),
                new CommandParam(true, "reason...")
            );
        }

        @Override
        public void execute(Message message, List<String> args) {
            TextChannel channel = message.getTextChannel();
            Map<String, Map<String, Object>> warns = settings.get("warns", Map.class);

            String mention = args.get(0);
            Member member = messages.parseMention(mention);
            if(member == null) {
                channel
                    .sendMessage("'" + mention + "' does not seem to represent a server member.")
                    .queue();
            }

            String id = member.getId();
            String reason = args.size() > 1 ? args.get(1) : "Unspecified";

            if(!warns.containsKey(id)) {
                warns.put(id, new LinkedHashMap<>());
            }

            Map<String, Object> map = warns.get(id);

            int count = (int)map.getOrDefault("count", 0) + 1;
            map.put("count", count);

            var reasons = (List<String>)map.getOrDefault("reasons", new ArrayList<String>());
            reasons.add(reason);
            map.put("reasons", reasons);

            settings.save();
            channel
                .sendMessage(mention + ", you have been warned *" + messages.warnMessage(count) + "* with a reason: " + reason)
                .queue();
        }
    };

    public static final Command[] ALL = values();

    public final String name;
    public final CommandPermission permission;
    public List<CommandParam> params = List.of();

    Command(String name, CommandPermission permission) {
        this.name = name;
        this.permission = permission;
    }

    public abstract void execute(Message message, List<String> args);

    public class CommandParam {
        public final boolean optional;
        public final String name;

        public CommandParam(boolean optional, String name) {
            this.optional = optional;
            this.name = name;
        }
    }

    public enum CommandPermission {
        /** Can be used for all guild members. */
        DEFAULT(null),

        /** Can only be used by administrators. */
        ADMIN_ONLY(Permission.ADMINISTRATOR),

        /** Can only be used by server owner or bot creator. */
        OWNER_ONLY(Permission.MANAGE_SERVER);

        public final Permission permission;

        CommandPermission(Permission permission) {
            this.permission = permission;
        }

        public boolean qualified(Member member) {
            return
                member.getUser().getIdLong() == creator().getIdLong() ||
                permission != null
                ?   member.hasPermission(permission)
                :   true;
        }
    }
}
