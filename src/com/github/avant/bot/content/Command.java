package com.github.avant.bot.content;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;

import java.awt.Color;
import java.io.*;
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

    JAVAC("javac", "Compiles a Java source file for release 8.", DEFAULT) {
        {
            params = List.of(
                new CommandParam(false, "classname"),
                new CommandParam(false, "program...")
            );
        }

        @Override
        public void execute(Message message, List<String> args) {
            TextChannel channel = message.getTextChannel();

            String name = args.get(0);
            String cname;
            String content = args.get(1);

            if(!name.endsWith(".java")) {
                cname = name + ".class";
                name += ".java";
            } else {
                cname = name.substring(0, name.lastIndexOf(".java")) + ".class";
            }

            content.replaceFirst("package (.|\\s)+;", "");

            File file = new File(CLASSES_DIR.getAbsolutePath(), name);
            try {
                var writer = new OutputStreamWriter(new FileOutputStream(file, false));
                writer.write(content);
                writer.close();

                String[] names = {name, cname};
                File compiled = new File(CLASSES_DIR.getAbsolutePath(), names[1]);

                channel
                    .sendMessage("Compiling...")
                    .flatMap(msg -> {
                        try {
                            Process process = Runtime.getRuntime().exec(new String[] {
                                "javac", "--release", "8", names[0]
                            }, null, CLASSES_DIR);

                            int excode = process.waitFor();
                            return switch(excode) {
                                case 0 -> channel
                                    .sendMessage("Here's your compiled `.class` file!")
                                    .addFile(compiled)
                                    .flatMap(m -> {
                                        return channel.sendMessage(
                                            "Make sure you have Java 8 or higher installed. " +
                                            "Open your command line, " +
                                            "`cd` to where the compiled file is located, " +
                                            "then run `java <filename>`."
                                        );
                                    });

                                default -> channel.sendMessage(String.format("Error compiling: `javac` exited with code `%d`.", excode));
                            };
                        } catch(Throwable t) {
                            return messages.error(message, t);
                        }
                    })
                    .map(msg -> {
                        file.delete();
                        compiled.delete();

                        return null;
                    })
                    .queue();
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
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
                if(member.getIdLong() == message.getAuthor().getIdLong()) {
                    message.getTextChannel()
                        .sendMessage("Can't warn yourself.")
                        .queue();
                } else {
                    warns.warn(message, member, args.size() > 1 ? args.get(1) : null);
                }
            }
        }
    },

    CLEARWARN("clearwarn", "Clears a server member's warnings", ADMIN_ONLY) {
        {
            params = List.of(
                new CommandParam(false, "member")
            );
        }

        @Override
        public void execute(Message message, List<String> args) {
            String mention = args.get(0);
            Member member;
            if((member = messages.memberExists(message, mention)) != null) {
                if(member.getIdLong() == message.getAuthor().getIdLong()) {
                    message.getTextChannel()
                        .sendMessage("Can't clearwarn yourself.")
                        .queue();
                } else {
                    warns.clearwarn(message, member);
                }
            }
        }
    },

    WARNINGS("warnings", "View a server member's or your warnings", ADMIN_ONLY) {
        {
            params = List.of(
                new CommandParam(true, "member")
            );
        }

        @Override
        public void execute(Message message, List<String> args) {
            String mention;
            Member offender = message.getMember();
            Member offended;

            if(args.size() > 0) {
                mention = args.get(0);
            } else {
                mention = offender.getAsMention();
            }

            if((offended = messages.memberExists(message, mention)) != null) {
                List<String> warnList = warns.warnings(offended);
                if(warnList.size() > 0) {
                    EmbedBuilder builder = new EmbedBuilder()
                        .setTitle(String.format("Warnings for %s#%s", offended.getEffectiveName(), offended.getUser().getDiscriminator()))
                        .setTimestamp(message.getTimeCreated())
                        .setColor(INFO)
                        .setAuthor(offender.getEffectiveName(), null, offender.getUser().getEffectiveAvatarUrl())
                        .setThumbnail(offended.getUser().getEffectiveAvatarUrl())
                        .setFooter(self(message.getGuild()).getEffectiveName() + "#" + prefix() + name, self().getEffectiveAvatarUrl());

                    StringBuilder buf = new StringBuilder();

                    for(int i = 0; i < warnList.size(); i++) {
                        buf
                            .append(String.format("**%d**: %s", i + 1, warnList.get(i)))
                            .append("\n");
                    }

                    message.getTextChannel()
                        .sendMessage(builder
                            .addField(String.format("%s#%s has %d warnings:",
                                offended.getEffectiveName(),
                                offended.getUser().getDiscriminator(),
                                warnList.size()
                            ),
                            buf.toString(), false)
                            .build()
                        )
                        .queue();
                } else {
                    message.getTextChannel()
                        .sendMessage(String.format("%s no warnings.",
                            args.size() > 0
                            ?   String.format("%s has", offended.getAsMention())
                            :   String.format("%s, you have", offended.getAsMention())
                        ))
                        .queue();
                }
            }
        }
    },

    RESTART("restart", "Exits the bot with code 1; bot will be restarted by the auto-run script.", OWNER_ONLY) {
        @Override
        public void execute(Message message, List<String> args) {
            Message res = message.getTextChannel()
                .sendMessage("Restarting...")
                .complete();

            settings.put("restart-message", String.format("%s-%s-%s", res.getGuild().getId(), res.getTextChannel().getId(), res.getId()));
            exit();
        }
    };

    public static final Command[] ALL = values();

    public static Color INFO = new Color(130, 91, 215);
    public static Color ERROR = new Color(215, 91, 91);

    public final String name;
    public final String description;

    public final CommandPermission permission;
    public List<CommandParam> params = List.of();

    private int minArgSize = -1;

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

    public int minArgSize() {
        if(minArgSize < 0) {
            minArgSize = 0;
            for(CommandParam param : params) {
                if(!param.optional) minArgSize++;
            }
        }
        return minArgSize;
    }

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
        DEFAULT {
            @Override
            public boolean qualified(Member member) {
                return true;
            }
        },

        /** Can only be used by administrators. */
        ADMIN_ONLY {
            @Override
            public boolean qualified(Member member) {
                return member.hasPermission(Permission.ADMINISTRATOR);
            }
        },

        /** Can only be used by server owner or bot creator. */
        OWNER_ONLY {
            @Override
            public boolean qualified(Member member) {
                User user = member.getUser();
                Member owner = getOwner(member.getGuild());

                return
                    user.getIdLong() == creator().getIdLong() ||
                    (
                        owner != null
                        ?   owner.getIdLong() == user.getIdLong()
                        :   false
                    );
            }
        };

        public abstract boolean qualified(Member member);
    }
}
