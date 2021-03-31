package com.github.avant.bot.content.minigames;

import java.awt.Graphics2D;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.function.*;

import javax.imageio.*;

import com.github.avant.bot.content.*;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.*;

import static com.github.avant.bot.AvantBot.*;

public class TicTacToe extends Minigame<TicTacToe, TicTacToe.TicTacToeModule> {
    public static final BufferedImage TILE;
    public static final BufferedImage CHECK_X;
    public static final BufferedImage CHECK_O;

    private static final Map<Integer, Integer> VALUES = new HashMap<>();

    {
        commands = List.of(
            Command.TICTACTOE_CHECK,
            Command.TICTACTOE_QUIT
        );
    }

    static {
        try {
            TILE = ImageIO.read(new File(ROOT_DIR.getAbsolutePath(), "tictactoe-tile.png"));
            CHECK_X = ImageIO.read(new File(ROOT_DIR.getAbsolutePath(), "tictactoe-check-x.png"));
            CHECK_O = ImageIO.read(new File(ROOT_DIR.getAbsolutePath(), "tictactoe-check-o.png"));

            VALUES.put(3, 3);
            VALUES.put(4, 4);
            VALUES.put(5, 4);
            VALUES.put(6, 4);
            VALUES.put(7, 5);
            VALUES.put(8, 5);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ByteArrayOutputStream STREAM = new ByteArrayOutputStream();

    @Override
    public TicTacToeModule create(Member... players) {
        return new TicTacToeModule(List.of(players));
    }

    public class TicTacToeModule extends Minigame<TicTacToe, TicTacToeModule>.MinigameModule<int[]> {
        protected Member[][] tiles;
        protected int width;
        protected int count;

        private Member winner;
        private boolean draw;
        private Map<String, BufferedImage> checkMap = new HashMap<>();

        public TicTacToeModule(List<Member> players) {
            super(players);
            checkMap.put(players.get(0).getId(), CHECK_X);
            checkMap.put(players.get(1).getId(), CHECK_O);
        }

        public void init(int width) {
            this.width = width;
            count = VALUES.get(Integer.valueOf(width));
            tiles = new Member[width][width];
        }

        public synchronized RestAction<Message> sendImage(Message message) {
            try {
                BufferedImage base = new BufferedImage(width * 64, width * 64, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = base.createGraphics();

                for(int x = 0; x < width; x++) {
                    for(int y = 0; y < width; y++) {
                        graphics.drawImage(TILE, x * 64, y * 64, null);

                        Member tile = tiles[x][y];
                        if(tile != null) {
                            graphics.drawImage(checkMap.get(tile.getId()), x * 64, y * 64, null);
                        }
                    }
                }

                STREAM.reset();
                ImageIO.write(base, "png", STREAM);

                graphics.dispose();
                return message.getTextChannel()
                    .sendMessage("Image preview:")
                    .addFile(STREAM.toByteArray(), "image.png");
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        public int getWidth() {
            return width;
        }

        public int getCount() {
            return count;
        }

        @Override
        public void execute(Message message, Member member, Supplier<int[]> supplier) {
            if(getCurrent().getIdLong() != member.getIdLong()) {
                message.getTextChannel()
                    .sendMessage(String.format("%s, now is not your turn!", member.getAsMention()))
                    .queue();

                return;
            }

            var i = supplier.get();
            int x = i[0] - 1, y = i[1] - 1;

            Member tile = tiles[x][y];
            if(tile != null) {
                message.getTextChannel()
                    .sendMessage(String.format("Tile `(%d, %d)` is already checked by %s", x + 1, y + 1, tile.getEffectiveName()))
                    .queue();

                return;
            } else {
                tiles[x][y] = tile = member;
                current++;

                message.getTextChannel()
                    .sendMessage(String.format("Tile `(%d, %d)` has been checked by %s", x + 1, y + 1, tile.getEffectiveName()))
                    .flatMap(this::sendImage)
                    .flatMap(msg -> {
                        check(x, y, member);
                        if(winner != null) {
                            stop(member.getGuild());
                            return msg.getTextChannel()
                                .sendMessage(String.format("Congratulations, %s. You won the game!", winner.getAsMention()));
                        } else if(draw) {
                            stop(member.getGuild());
                            return msg.getTextChannel().sendMessage("It's a draw!");
                        } else {
                            return notifyTurn(msg);
                        }
                    })
                    .queue();
            }
        }

        protected void check(int x, int y, Member member) {
            for(int i = 0; i < width; i++) {
                if(tiles[x][i] != member) break;

                if(i == count - 1) {
                    winner = member;
                    return;
                }
            }

            for(int i = 0; i < width; i++) {
                if(tiles[i][y] != member) break;

                if(i == count - 1) {
                    winner = member;
                    return;
                }
            }

            if(x == y) {
                for(int i = 0; i < width; i++) {
                    if(tiles[i][i] != member) break;

                    if(i == count - 1) {
                        winner = member;
                        return;
                    }
                }
            }

            if(x + y == width - 1) {
                for(int i = 0; i < width; i++) {
                    if(tiles[i][(width - 1) - i] != member) break;

                    if(i == count - 1) {
                        winner = member;
                        return;
                    }
                }
            }

            if(current == width * width) {
                draw = true;
            }
        }
    }
}
