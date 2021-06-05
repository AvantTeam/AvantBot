package bot.core;

import bot.content.*;
import bot.content.Command.*;

import org.slf4j.*;

import net.dv8tion.jda.api.entities.*;

import java.util.*;
import java.util.regex.*;

import static bot.AvantBot.*;

public class Commands {
    private static final Logger LOG = LoggerFactory.getLogger(Commands.class);

    private final Pattern splitter = Pattern.compile("(?:```(?:.|[\\s\\S])*```)|(?:\".+\")|(?:[^\\s]+)");

    public Commands() {
        LOG.debug("Initialized command handler.");
    }

    public void handle(Message message) {
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
            if(!command.hidden) {
                exec(message, command, split);
            } else {
                Minigame<?, ?>.MinigameModule<?> module = null;

                for(var game : Minigame.getAll()) {
                    module = game.current(message.getGuild());
                    if(module != null && module.getCommands().contains(command)) {
                        break;
                    }
                }

                if(module != null) {
                    exec(message, command, split);
                } else {
                    message.getTextChannel()
                        .sendMessage(String.format("You can't use `%s%s` at the moment.", prefix(), command.name))
                        .queue();
                }
            }
        }
    }

    protected void exec(Message message, Command command, List<String> args) {
        int size = command.minArgSize();
        if(args.size() < size) {
            message.getTextChannel()
                .sendMessage(String.format("Insufficient amount of arguments *(supplied: %d, required: %d)*.", args.size(), size))
                .queue();
        } else {
            for(int i = 0; i < Math.min(command.params.size(), args.size()); i++) {
                CommandParam param = command.params.get(i);
                String arg = args.get(i);

                if(!param.reserved.isEmpty() && !param.reserved.contains(arg)) {
                    StringBuilder builder = new StringBuilder();
                    int c = 0;
                    for(; c < param.reserved.size(); c++) {
                        if(param.reserved.size() > 1 && c == param.reserved.size() - 1) builder.append("and ");

                        builder.append("`").append(param.reserved.get(c)).append("`");

                        if(param.reserved.size() > 2 && c < param.reserved.size() - 1) {
                            builder.append(", ");
                        } else if(param.reserved.size() == 2) {
                            builder.append(" ");
                        }
                    }

                    message.getTextChannel()
                        .sendMessage(String.format("Invalid parameter `%d`: `%s`. Only %s %s accepted.",
                            i, arg, builder, "are"
                        ))
                        .queue();

                    return;
                }
            }

            command.execute(message, args);
        }
    }
}
