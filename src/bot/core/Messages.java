package bot.core;

import bot.content.*;
import bot.utils.exception.*;

import org.slf4j.*;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.requests.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import static bot.AvantBot.*;

public class Messages extends ListenerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(Messages.class);
    private static final ByteArrayOutputStream STREAM = new ByteArrayOutputStream();
    private static final PrintStream PRINT = new PrintStream(STREAM);

    private static final String[] WARNS = { "once", "twice", "thrice", "four times", "too many times" };

    public Messages() {
        LOG.debug("Initialized message listener.");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        List<Attachment> attachments = event.getMessage().getAttachments();

        if(msg.getAuthor().isBot()) return;

        LOG.info(String.valueOf(attachments.size()));
        if(event.getTextChannel().getId().equals(channel("wastelands-rooms")) && attachments.size() == 0){
            try {
                msg.delete().queue();
                msg.getAuthor().openPrivateChannel().flatMap(channel -> channel
                    .sendMessage("Only send valid room files in the #wastelands-rooms channel. Send them as .wrd files.")
                ).queue();
            } catch(Exception e){
                LOG.error(e.toString());
            }
        }

        if(msg.getChannel() instanceof PrivateChannel) return;

        Member member = event.getMember();

        for(Attachment attachment : attachments) {
            String extension = attachment.getFileExtension();
            if(extension != null && extension.equals("wrd")){
                attachment.retrieveInputStream().thenAcceptAsync(input -> {
                    try(BufferedReader reader = new BufferedReader(new InputStreamReader(input))){
                        String line;
                        while((line = reader.readLine()) != null){
                            tileRenderer.renderFile(tiles, attachment, msg, line).thenAccept(RestAction::queue);
                        }
                        msg.delete().queue();
                    } catch(Exception e) {
                        if(event.getTextChannel().getId().equals(channel("wastelands-rooms"))){
                            try {
                                msg.delete().queue();
                                msg.getAuthor().openPrivateChannel().flatMap(channel -> channel
                                    .sendMessage("Only send valid room files in the #wastelands-rooms channel. Send them as `.wrd` files.")
                                ).queue();
                            } catch(Exception err){
                                LOG.error(err.toString());
                            }
                        } else {
                            event.getTextChannel()
                                .sendMessage("The sent `.wrd` file is broken or invalid.")
                                .queue();
                        }
                    }
                });
            }
        }

        assert member != null;
        LOG.debug("{}#{} in #{}: {}", member.getEffectiveName(), member.getUser().getDiscriminator(), event.getTextChannel().getName(), msg.getContentDisplay());
        try {
            commands.handle(msg);
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

    public Integer validNumber(Message message, String number, int min, int max) {
        return assertMessage(
            message,
            () -> Integer.parseInt(number),
            (Integer res, Member member) -> res >= min && res <= max,
            (Integer res, Member member) -> {
                if(res == null) {
                    return String.format("%s, '%s' does not seem to represent a number.", member.getAsMention(), number);
                } else {
                    return String.format("%s, the number must be *from* %d *to* %d.", member.getAsMention(), min, max);
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
        return WARNS[Math.min(Math.max(i - 1, 0), WARNS.length - 1)];
    }

    public User parseMention(String mention) {
        String strip = mention;
        if(strip.startsWith("<@") && strip.endsWith(">")) strip = strip.substring(2, mention.length() - 1);
        if(strip.startsWith("!")) strip = strip.substring(1);

        return getUser(strip);
    }
}
