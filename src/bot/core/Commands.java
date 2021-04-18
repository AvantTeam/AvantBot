package bot.core;

import bot.content.*;

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
            command.execute(message, args);
        }
    }
}
