package bot.content;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.*;

import org.slf4j.*;

import static bot.AvantBot.*;

public class TileRenderer {
    private static final Logger LOG = LoggerFactory.getLogger(TileRenderer.class);
    private static final ByteArrayOutputStream STREAM = new ByteArrayOutputStream();

    public static File tilePath = new File(ROOT_DIR, "map-tiles/");

    public Map<String, BufferedImage> loadTiles(){
        Map<String, BufferedImage> tiles = new HashMap<>();

        File[] directoryListing = tilePath.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                try {
                    if (child.isFile() && child.toPath().toString().endsWith(".png")) {
                        tiles.put(child.toString(), ImageIO.read(child));
                    }
                } catch(Exception e){
                    LOG.error(e.toString());
                }
            }
        } else {
            LOG.error("The given tile directory isn't a valid directory!");
        }

        return tiles;
    }

    public void renderFile(Map<String, BufferedImage> tiles, Message message, String data){
        try {
            String[] splitData = data.split(";");

            int[] size = Arrays.stream(splitData[0].split("\\.")).mapToInt(Integer::parseInt).toArray();
            String[] tileData = Arrays.copyOfRange(splitData, 1, splitData.length);

            BufferedImage outputImage = new BufferedImage(size[0] * 16, size[1] * 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = outputImage.createGraphics();

            int x = 0;
            int y = 0;
            LOG.debug(tiles.toString());
            for (String row : tileData) {
                String[] rowData = row.split("\\.");
                for (String tile : rowData) {
                    graphics.drawImage(tiles.get(tile), x * 16, y * 16, null);
                    x++;
                }
                y++;
            }

            STREAM.reset();
            ImageIO.write(outputImage, "png", STREAM);

            graphics.dispose();
            message.getTextChannel().sendMessage("Room preview:").addFile(STREAM.toByteArray(), "image.png").queue();
        } catch(Exception e){
            LOG.error(e.toString());
        }
    }
}