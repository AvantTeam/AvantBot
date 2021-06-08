package bot.content;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.imageio.*;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.*;

import net.dv8tion.jda.api.requests.restaction.*;
import org.slf4j.*;

import static bot.AvantBot.*;

public class TileRenderer {
    private static final Logger LOG = LoggerFactory.getLogger(TileRenderer.class);
    private static final File outputFile = new File(ROOT_DIR, "tile-generated-image.png");

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

    public CompletableFuture<MessageAction> renderFile(Map<String, BufferedImage> tiles, Attachment attachment, Message message, String data) {
        return attachment.downloadToFile().thenApply(attach -> {
            synchronized(TileRenderer.class){
                LOG.debug("Creating preview for {}", data);
                try{
                    String[] split = data.split(";");

                    String name = split[0];
                    int[] size = Arrays.stream(split[1].split("\\.")).mapToInt(Integer::parseInt).toArray();
                    String[] tileData = Arrays.copyOfRange(split, 2, split.length);

                    BufferedImage outputImage = new BufferedImage(size[0] * 16, size[1] * 16, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = outputImage.createGraphics();

                    for(int y = 0; y < tileData.length; y++){
                        String[] rows = tileData[y].split("\\.");
                        for(int x = 0; x < rows.length; x++){
                            graphics.drawImage(tiles.get(rows[x]), x * 16, y * 16, null);
                        }
                    }

                    int w = outputImage.getWidth();
                    int h = outputImage.getHeight();

                    BufferedImage scaledOutputImage = new BufferedImage(w * 2, h * 2, BufferedImage.TYPE_INT_ARGB);

                    AffineTransform at = new AffineTransform();

                    at.scale(2.0, 2.0);
                    AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

                    scaledOutputImage = scaleOp.filter(outputImage, scaledOutputImage);

                    ImageIO.write(scaledOutputImage, "png", outputFile);

                    graphics.dispose();

                    EmbedBuilder eb = new EmbedBuilder()
                        .setTitle(name, null)
                        .setDescription("Preview:")
                        .setColor(new Color(121, 239, 148))
                        .setAuthor(message.getAuthor().getName(), null, message.getAuthor().getAvatarUrl())
                        .setImage("attachment://" + outputFile.getName());

                    return message.getTextChannel()
                        .sendFile(attach)
                        .addFile(outputFile)
                        .embed(eb.build());
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
