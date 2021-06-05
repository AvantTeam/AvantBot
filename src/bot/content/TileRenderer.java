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

    public Map<String, BufferedImage> loadTiles() {
        Map<String, BufferedImage> tiles = new HashMap<>();

        File[] directoryListing = tilePath.listFiles();
        if(directoryListing != null) {
            for(File child : directoryListing) {
                try {
                    if(child.isFile() && child.getName().endsWith(".png")) {
                        String name = child.getName();
                        int i = name.lastIndexOf(".");
                        name = name.substring(0, i);

                        tiles.put(name, ImageIO.read(child));
                    }
                } catch(Exception e){
                    LOG.error("An error occurred while reading a file", e);
                }
            }
        } else {
            LOG.error("The given tile directory isn't a valid directory!");
        }

        return tiles;
    }

    public MessageAction renderFile(Map<String, BufferedImage> tiles, Message message, String data) {
        synchronized(TileRenderer.class){
            LOG.debug("Creating preview for {}", data);
            try{
                String[] split = data.split(";");

                int[] size = Arrays.stream(split[0].split("\\.")).mapToInt(Integer::parseInt).toArray();
                String[] tileData = Arrays.copyOfRange(split, 1, split.length);

                BufferedImage outputImage = new BufferedImage(size[0] * 16, size[1] * 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = outputImage.createGraphics();

                for(int y = 0; y < tileData.length; y++){
                    String[] rows = tileData[y].split("\\.");
                    for(int x = 0; x < rows.length; x++){
                        graphics.drawImage(tiles.get(rows[x]), x * 16, y * 16, null);
                    }
                }

                STREAM.reset();
                ImageIO.write(outputImage, "png", STREAM);

                graphics.dispose();
                return message.getTextChannel()
                    .sendMessage("Room preview:")
                    .addFile(STREAM.toByteArray(), "image.png");
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }
}
