package com.github.avant.bot.content;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;

import java.util.*;

import static com.github.avant.bot.AvantBot.*;
import static com.github.avant.bot.content.Command.CommandPermission.*;

public enum Command {
    HELP("help", DEFAULT) {
        @Override
        public void execute(Message message, List<String> args) {
            
        }
    };

    public static final Command[] ALL = values();

    public final String name;
    public final CommandPermission permission;

    Command(String name, CommandPermission permission) {
        this.name = name;
        this.permission = permission;
    }

    public abstract void execute(Message message, List<String> args);

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
