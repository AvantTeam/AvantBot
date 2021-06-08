package bot.content;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.*;

import java.awt.Color;
import java.io.*;
import java.util.*;
import java.util.zip.*;

import bot.content.minigames.TicTacToe.*;
import bot.core.*;
import bot.utils.exception.*;

import static bot.AvantBot.*;
import static bot.content.Command.CommandPermission.*;

public enum Command {
    HELP("help", "Shows all server commands that you may use.", DEFAULT) {
        {
            params = List.of(new CommandParam(true, "command"));
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
                    if(command.hidden) {
                        message.getTextChannel()
                            .sendMessage(String.format("You can't use `%s%s` at the moment.", prefix(), command.name))
                            .queue();

                        return;
                    } else {
                        builder
                            .setTitle(prefix() + command.name)
                            .setDescription(command.description);

                        builder.addField("Usage:", command.toString(), false);
                    }
                }
            } else {
                builder
                    .setTitle("Server commands")
                    .setDescription("List of server commands that you may use.");

                for(Command command : ALL) {
                    if(!command.hidden && command.permission.qualified(member)) {
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
            params = List.of(new CommandParam(false, "classname"), new CommandParam(false, "program..."));
        }

        @Override
        public void execute(Message message, List<String> args) {
            TextChannel channel = message.getTextChannel();

            String name = args.get(0);
            String nameRaw;
            String content = args.get(1);

            if(!name.endsWith(".java")) {
                nameRaw = name;
                name = nameRaw + ".java";
            } else {
                nameRaw = name.substring(0, name.lastIndexOf(".java"));
            }

            content = content.replaceFirst("package (.|\\s)+;", "");

            File parent = new File(CLASSES_DIR.getAbsolutePath(), nameRaw);
            parent.mkdirs();
            File file = new File(parent.getAbsolutePath(), name);

            try {
                var writer = new OutputStreamWriter(new FileOutputStream(file, false));
                writer.write(content);
                writer.close();

                String[] n = {name, nameRaw};
                channel
                    .sendMessage("Compiling...")
                    .flatMap(msg -> {
                        try {
                            Process process = Runtime.getRuntime().exec(new String[] {
                                "javac", "--release", "8", n[0]
                            }, null, parent);

                            int excode = process.waitFor();
                            return switch(excode) {
                                case 0 -> 
                                    channel
                                        .sendMessage("Done compiling.")
                                        .flatMap(m -> {
                                            File zipfile = new File(parent.getAbsolutePath(), nameRaw + ".zip");
                                            try(ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(zipfile))) {
                                                for(File src : parent.listFiles()) {
                                                    if(!src.getName().endsWith(".class")) continue;

                                                    try(FileInputStream in = new FileInputStream(src)) {
                                                        ZipEntry zipEntry = new ZipEntry(src.getName());
                                                        stream.putNextEntry(zipEntry);

                                                        byte[] bytes = new byte[1024];
                                                        int length;
                                                        while((length = in.read(bytes)) >= 0) {
                                                            stream.write(bytes, 0, length);
                                                        }
                                                    }
                                                }
                                            } catch(Throwable t) {
                                                return messages.error(message, t);
                                            }

                                            return channel
                                                .sendMessage("Here's your compiled `.class` file in a zipped file!")
                                                .addFile(zipfile);
                                        })
                                        .flatMap(m -> {
                                            return channel.sendMessage(String.format(
                                                "Make sure you have Java 8 or higher installed. " +
                                                "Open your command line, " +
                                                "`cd` to where the compiled file is located, " +
                                                "then run `java %s`.",
                                                n[1]
                                            ));
                                        });

                                default -> channel.sendMessage(String.format("Error compiling: `javac` exited with code `%d`.", excode));
                            };
                        } catch(Throwable t) {
                            return messages.error(message, t);
                        }
                    })
                    .map(msg -> {
                        file.delete();

                        for(File fi : parent.listFiles()) fi.delete();
                        parent.delete();

                        return null;
                    })
                    .queue();
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
        }
    },

    TICTACTOE("ttt", "Plays a tictactoe game.", DEFAULT) {
        {
            params = List.of(new CommandParam(false, "member"), new CommandParam(false, "width"));
        }

        @Override
        public void execute(Message message, List<String> args) {
            Member member = message.getMember();
            Member opponent;
            Integer width;

            if(
                (opponent = messages.memberExists(message, args.get(0))) != null &&
                (width = messages.validNumber(message, args.get(1), 3, 8)) != null
            ) {
                if(member.getIdLong() == opponent.getIdLong()) {
                    message
                        .getTextChannel()
                        .sendMessage("Can't play with yourself, get a friend.")
                        .queue();
                } else {
                    try {
                        TicTacToeModule module = tictactoe.start(member, opponent);
                        module.init(width);

                        message.getTextChannel()
                            .sendMessage(String.format(
                                "Starting a TicTacToe game, %s against %s! The check target count is %d.",
                                member.getAsMention(),
                                opponent.getAsMention(),
                                module.getCount()
                            ))
                            .flatMap(module::sendImage)
                            .flatMap(msg -> message.getTextChannel().sendMessage(String.format(
                                "Use %s to check the board. Note that `(1, 1)` is the board's top-left corner.", Command.TICTACTOE_CHECK
                            )))
                            .flatMap(module::notifyTurn)
                            .queue();
                    } catch(IllegalArgumentException e) {
                        throw new CommandException(this, e.getMessage());
                    }
                }
            }
        }
    },

    TICTACTOE_CHECK("tttc", DEFAULT) {
        {
            params = List.of(new CommandParam(false, "column"), new CommandParam(false, "row"));
        }

        @Override
        public void execute(Message message, List<String> args) {
            Member member = message.getMember();

            var module = tictactoe.current(message.getGuild());
            if(module != null) {
                int[] position;
                if((position = messages.assertMessage(
                    message,
                    () -> new int[]{
                        Integer.parseInt(args.get(0)),
                        Integer.parseInt(args.get(1))
                    },

                    (int[] pos, Member m) ->
                        pos[0] <= module.getWidth() && pos[0] > 0 &&
                        pos[1] <= module.getWidth() && pos[1] > 0,

                    (int[] pos, Member m) -> String.format(
                        "%s, `(%s, %s)` must be inclusively between `(1, 1)` and `(%d, %d)`.",
                        member.getAsMention(),
                        args.get(0), args.get(1),
                        module.getWidth(), module.getWidth()
                    )
                )) != null) {
                    module.execute(message, message.getMember(), () -> new int[]{ position[0], position[1] });
                }
            }
        }
    },

    TICTACTOE_QUIT("tttq", DEFAULT) {
        @Override
        public void execute(Message message, List<String> args) {
            var module = tictactoe.current(message.getGuild());
            if(module != null) {
                tictactoe.stop(message.getGuild());
                message.getTextChannel()
                    .sendMessage("Stopped the current TicTacToe game.")
                    .queue();
            }
        }
    },

    WARN("warn", "Warns a server member.", ADMIN_ONLY) {
        {
            params = List.of(new CommandParam(false, "member"), new CommandParam(true, "reason..."));
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
            params = List.of(new CommandParam(false, "member"));
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
            params = List.of(new CommandParam(true, "member"));
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    CONFIG("config", "Gets/sets the bot's settings configuration", ADMIN_ONLY) {
        {
            params = List.of(
                new CommandParam(false, "get/set", "get", "set"),
                new CommandParam(false, "key..."),
                new CommandParam(true, "value")
            );
        }

        @Override
        public void execute(Message message, List<String> args) {
            String access = args.remove(0);
            boolean set;
            Object object = null;
            Object value = null;

            if(set = access.equals("set")) value = args.remove(args.size() - 1);

            StringBuilder config = new StringBuilder();
            String lastArg = null;
            for(int i = 0; i < args.size(); i++) {
                String arg = args.get(i);

                if(!set || i < args.size() - 1){
                    if(object == null) {
                        object = settings.get(arg);
                    } else if(object instanceof Map map) {
                        try {
                            object = map.get(arg);
                        } catch(ClassCastException e) {
                            object = null;
                        }
                    } else {
                        object = null;
                    }

                    if(object == null) {
                        if(config.isEmpty()) {
                            StringBuilder builder = new StringBuilder()
                                .append("No such property `")
                                .append(arg)
                                .append("`.");

                            message.getTextChannel()
                                .sendMessage(builder)
                                .queue();
                        } else {
                            StringBuilder builder = new StringBuilder()
                                .append("No such property `")
                                .append(arg)
                                .append("` in configuration `")
                                .append(config)
                                .append("`.");

                            message.getTextChannel()
                                .sendMessage(builder)
                                .queue();
                        }

                        return;
                    } else {
                        if(i > 0) config.append(".");
                        config.append(arg);
                    }
                }

                lastArg = arg;
            }

            if(access.equals("get")) {
                StringBuilder builder = new StringBuilder()
                    .append("Configuration `")
                    .append(config)
                    .append("` is currently `")
                    .append(object)
                    .append("`.");

                message.getTextChannel()
                    .sendMessage(builder)
                    .queue();
            } else {
                String canonical = "";
                if(!config.isEmpty()) canonical += config + ".";
                canonical += lastArg;

                try {
                    if(object instanceof Map map) {
                        map.put(lastArg, value);
                    } else if(object == null || config.isEmpty()) {
                        settings.put(lastArg, value);
                    }

                    StringBuilder builder = new StringBuilder()
                        .append("`")
                        .append(canonical)
                        .append("` is now `")
                        .append(value)
                        .append("`.");

                    message.getTextChannel()
                        .sendMessage(builder)
                        .queue();
                } catch(Throwable t) {
                    StringBuilder builder = new StringBuilder()
                        .append("Value `")
                        .append(value)
                        .append("` isn't assignable to `")
                        .append(canonical)
                        .append("`.");

                    message.getTextChannel()
                        .sendMessage(builder)
                        .queue();
                }
            }
        }
    },

    SETTINGS("settings", "Gets/sets the bot's settings file", OWNER_ONLY) {
        {
            params = List.of(new CommandParam(false, "get/set", "get", "set"));
        }

        @Override
        public void execute(Message message, List<String> args) {
            if(args.get(0).equals("get")) {
                message.getAuthor().openPrivateChannel().flatMap(channel -> channel
                    .sendMessage("The currently used bot settings file:")
                    .addFile(settings.getFile())
                    .flatMap(msg -> message.getTextChannel()
                        .sendMessage("Sent the settings file.")
                    )
                ).queue();
            } else {
                Attachment file = null;
                for(Attachment att : message.getAttachments()) {
                    if(!att.isImage() && !att.isVideo()) {
                        file = att;
                    }
                }

                if(file == null) {
                    message.getTextChannel()
                        .sendMessage("Settings file must be a JSON file.")
                        .queue();
                } else {
                    file.retrieveInputStream().thenAccept(input -> { try {
                        settings.setFile(input);
                        message.getTextChannel()
                            .sendMessage("Successfully set the settings file.")
                            .queue();
                    } catch(IOException e) {
                        message.getTextChannel()
                            .sendMessage("The sent settings file is broken or not a valid JSON file.")
                            .queue();
                    }});
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
            exit(1);
        }
    },

    EXIT("exit", "Exits the bot with code 0. Will not restart.", OWNER_ONLY){
        @Override
        public void execute(Message message, List<String> args) {
            message.getTextChannel()
                .sendMessage("Exiting...")
                .queue();

            exit(0);
        }
    };

    public static final Command[] ALL = values();

    public static Color INFO = new Color(130, 91, 215);
    public static Color ERROR = new Color(215, 91, 91);

    public final String name;
    public final String description;
    public final boolean hidden;

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
        hidden = false;
        this.permission = permission;
    }

    Command(String name, CommandPermission permission) {
        this.name = name;
        this.description = null;
        this.hidden = true;
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

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder()
            .append("`")
            .append(prefix())
            .append(name)
            .append(" ");

        for(int i = 0; i < params.size(); i++) {
            buf.append(params.get(i));
            if(i < params.size() - 1) {
                buf.append(" ");
            }
        }

        buf.append("`");
        return buf.toString();
    }

    public static class CommandParam {
        public final boolean optional;
        public final String name;
        public final List<String> reserved;

        public CommandParam(boolean optional, String name, String... reserved) {
            this.optional = optional;
            this.name = name;
            this.reserved = List.of(reserved);
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
                    owner != null && owner.getIdLong() == user.getIdLong();
            }
        };

        public abstract boolean qualified(Member member);
    }
}
