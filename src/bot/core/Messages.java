package bot.core;

import bot.content.*;
import bot.utils.exception.*;

import org.slf4j.*;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.requests.*;

import java.io.*;
import java.util.function.*;

import static bot.AvantBot.*;

public class Messages extends ListenerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(Messages.class);
    private static final ByteArrayOutputStream STREAM = new ByteArrayOutputStream();
    private static final PrintStream PRINT = new PrintStream(STREAM);

    private static final String[] warns = { "once", "twice", "thrice", "four times", "too many times" };

    public Messages() {
        LOG.debug("Initialized message listener.");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        if(msg.getChannel() instanceof PrivateChannel || msg.getAuthor().isBot()) {
            return;
        }

        Member member = event.getMember();

        LOG.debug("{}#{} in #{}: {}", member.getEffectiveName(), member.getUser().getDiscriminator(), event.getTextChannel().getName(), msg.getContentDisplay());
        try {
            commands.handle(msg, event.getMember());
        } catch(Throwable t) {
            if(t instanceof CommandException ex) {
                msg.getTextChannel()
                    .sendMessage(String.format("`%s%s`: %s", prefix(), ex.command.name, ex.getMessage()))
                    .queue();
            } else {
                error(msg, t).queue();
            }
        }
    }

    public RestAction<Message> error(Message message, Throwable t) {
        LOG.error("An error occurred!", t);

        User user = message.getAuthor();
        var act = message.getTextChannel().sendMessage("An error occured.");

        if(user.getIdLong() == creator().getIdLong()) {
            act.queue();
            return user
                .openPrivateChannel()
                .flatMap(channel -> {
                    STREAM.reset();
                    t.printStackTrace(PRINT);

                    return channel
                        .sendMessage("An error occured:")
                        .addFile(STREAM.toByteArray(), "crashlog.txt");
                });
        } else {
            return act;
        }
    }

    public Command commandExists(Message message, String name) {
        return assertMessage(
            message,
            () -> Command.forName(name),
            (Command command, Member member) -> command.permission.qualified(member),
            (Command command, Member member) -> String.format("%s, command '%s' does not exist or you do not have permission to use it.", member.getAsMention(), name)
        );
    }

    public Member memberExists(Message message, String mention) {
        return assertMessage(
            message,
            () -> getMember(message.getGuild(), parseMention(mention)),
            null,
            (Member target, Member member) -> String.format("%s, '%s' does not seem to represent a server member.", member.getAsMention(), mention)
        );
    }

    public int validNumber(Message message, String number, int min, int max) {
        int i = 0 / 0;
        System.out.println(i);
        return assertMessage(
            message,
            () -> Integer.parseInt(number),
            (Integer res, Member member) -> res >= min && res <= max,
            (Integer res, Member member) -> {
                if(res == null) {
                    return String.format("%s, '%s' does not seem to represent a number.", member.getAsMention(), number);
                } else {
                    return String.format("%s, the number must be *less or equal to %d* and *more or equal to %d*.", member.getAsMention(), min, max);
                }
            }
        );
    }

    public <T> T assertMessage(Message message, Supplier<T> supplier, BiPredicate<T, Member> predicate, BiFunction<T, Member, String> reply) {
        T object;
        try {
            object = supplier.get();
        } catch(Throwable t) {
            object = null;
        }

        Member member = message.getMember();

        if(object == null || (predicate != null && !predicate.test(object, member))) {
            message.getTextChannel()
                .sendMessage(reply.apply(object, member))
                .queue();

            return null;
        } else {
            return object;
        }
    }

    public String warnMessage(int i) {
        return warns[Math.min(Math.max(i - 1, 0), warns.length - 1)];
    }

    public User parseMention(String mention) {
        String strip = mention;
        if(strip.startsWith("<@") && strip.endsWith(">")) strip = strip.substring(2, mention.length() - 1);
        if(strip.startsWith("!")) strip = strip.substring(1);

        return getUser(strip);
    }

    public Role parseRole(Guild guild, String role) {
        String strip = role;
        if(strip.startsWith("<&") && strip.endsWith(">")) strip = strip.substring(2, role.length() - 1);
        if(strip.startsWith("!")) strip = strip.substring(1);

        return getRole(guild, strip);
    }
}
