package com.github.avant.bot.content;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;

import java.awt.Color;
import java.util.*;

import static com.github.avant.bot.AvantBot.*;
import static com.github.avant.bot.content.Command.CommandPermission.*;

public enum Command {
    HELP("help", "Shows all server commands that you may use.", DEFAULT) {
        {
            params = List.of(
                new CommandParam(true, "command")
            );
        }

        @Override
        public void execute(Message message, List<String> args) {
            Guild guild = message.getGuild();
            Member member = message.getMember();

            EmbedBuilder builder = new EmbedBuilder()
                .setTimestamp(message.getTimeCreated())
                .setColor(INFO)
                .setAuthor(member.getEffectiveName(), null, member.getUser().getEffectiveAvatarUrl())
                .setFooter(self(guild).getEffectiveName() + "#" + prefix() + name, self().getEffectiveAvatarUrl());

            if(args.size() > 0) {
                Command command;
                if((command = messages.commandExists(message, args.get(0))) != null) {
                    builder
                        .setTitle(prefix() + command.name)
                        .setDescription(command.description);

                    StringBuilder buf = new StringBuilder()
                        .append("`")
                        .append(prefix() + command.name)
                        .append(" ");

                    for(int i = 0; i < command.params.size(); i++) {
                        buf.append(command.params.get(i));
                        if(i < command.params.size() - 1) {
                            buf.append(" ");
                        }
                    }

                    buf.append("`");
                    builder.addField("Usage:", buf.toString(), false);
                }
            } else {
                builder
                    .setTitle("Server commands")
                    .setDescription("List of server commands that you may use.");

                for(Command command : ALL) {
                    if(command.permission.qualified(member)) {
                        builder.addField(prefix() + command.name, command.description, false);
                    }
                }
            }

            message.getTextChannel()
                .sendMessage(builder.build())
                .queue();
        }
    },

    WARN("warn", "Warns a server member.", ADMIN_ONLY) {
        {
            params = List.of(
                new CommandParam(false, "member"),
                new CommandParam(true, "reason...")
            );
        }

        @Override
        public void execute(Message message, List<String> args) {
            String mention = args.get(0);
            Member member;
            if((member = messages.memberExists(message, mention)) != null) {
                warns.warn(message, member, args.size() > 1 ? args.get(1) : null);
            }
        }
    };

    public static final Command[] ALL = values();

    public static Color INFO = new Color(130, 91, 215);
    public static Color ERROR = new Color(215, 91, 91);

    public final String name;
    public final String description;

    public final CommandPermission permission;
    public List<CommandParam> params = List.of();

    public static Command forName(String name) {
        for(Command command : ALL) {
            if(command.name.equals(name)) return command;
        }
        return null;
    }

    Command(String name, String description, CommandPermission permission) {
        this.name = name;
        this.description = description;
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

        @Override
        public String toString() {
            return (optional ? '[' : '<') + name + (optional ? ']' : '>');
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
