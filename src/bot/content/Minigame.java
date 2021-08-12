package bot.content;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.*;

public abstract class Minigame<T extends Minigame<T, M>, M extends Minigame<T, M>.MinigameModule<?>> {
    private final Map<String, M> modules = new HashMap<>();
    protected List<Command> commands = List.of();

    private static final List<Minigame<?, ?>> all = new ArrayList<>();

    {
        all.add(this);
    }

    public static List<Minigame<?, ?>> getAll() {
        return all;
    }

    public M current(Guild guild) {
        return modules.get(guild.getId());
    }

    public M start(Member... players) {
        if(players == null || players.length <= 0) throw new IllegalArgumentException("There must be one player or more.");

        Guild guild = players[0].getGuild();

        var current = current(guild);
        if(current == null) {
            var game = create(players);
            modules.put(guild.getId(), game);

            return game;
        } else {
            throw new IllegalArgumentException(String.format("There already is a %s minigame going on.", getClass().getSimpleName()));
        }
    }

    public void stop(Guild guild) {
        var current = current(guild);
        if(current != null) {
            modules.remove(guild.getId());
        } else {
            throw new IllegalArgumentException(String.format("There is no %s minigame going on.", getClass().getSimpleName()));
        }
    }

    public abstract M create(Member... players);

    public abstract class MinigameModule<S> {
        private final List<Member> players;
        protected int current;

        public MinigameModule(List<Member> players) {
            this.players = players;
        }

        public List<Member> getPlayers() {
            return players;
        }

        public Member getCurrent() {
            return get(current);
        }

        public Member get(int current) {
            return players.get(current % players.size());
        }

        public RestAction<Message> notifyTurn(Message message) {
            return message.getTextChannel()
                .sendMessage(String.format("Now is your turn, %s!\nIf you don't respond in 5 minutes, the game will automatically end.", getCurrent().getAsMention()))
                .delay(5, TimeUnit.MINUTES)
                .flatMap(msg -> {
                    stop(msg.getGuild());
                    return unresponsive(msg, get(current));
                });
        }

        public RestAction<Message> unresponsive(Message message, Member unresponsive) {
            return message.getTextChannel()
                .sendMessage(String.format("Due to %s being unresponsive, %s won the game!", get(current).getEffectiveName(), get(current + 1).getAsMention()));
        }

        public List<Command> getCommands() {
            return commands;
        }

        public void execute(Message message, Member member, Supplier<S> supplier) {}
    }
}
