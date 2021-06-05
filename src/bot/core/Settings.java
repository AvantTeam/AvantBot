package bot.core;

import bot.utils.*;

import org.slf4j.*;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;

import static bot.AvantBot.*;

@SuppressWarnings("unchecked")
public class Settings {
    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

    public static final TypeReference<Map<String, Object>> REF_MAP = new TypeReference<>() {};

    private final ObjectMapper mapper;
    private final File file;
    private final Map<String, Object> map;

    public Settings() {
        LOG.debug("Initializing bot settings.");

        mapper = new ObjectMapper();
        file = new File(ROOT_DIR.getAbsolutePath(), "settings.json");

        map = new LinkedHashMap<>();

        setDefaults();

        LOG.debug("Initialized bot settings.");
    }

    public void setFile(InputStream stream) throws IOException {
        Map<String, Object> ref = new LinkedHashMap<>();
        read(stream, ref);

        map.clear();
        map.putAll(ref);
        save();
    }

    private void setDefaults() {
        read();

        if(!has("prefix")) {
            put("prefix", "!");
        }
        if(!has("channels")) {
            Map<String, String> channels = new LinkedHashMap<>();
            channels.put("moderation", "785671460408655922");
            channels.put("wastelands-rooms", "850733600411615252");

            put("channels", channels);
        }
        if(!has("warns")) {
            put("warns", new LinkedHashMap<String, Map<String, Object>>());
        }
    }

    private void read() {
        if(file.exists()) {
            try(InputStream stream = new FileInputStream(file)) {
                read(stream, map);
            } catch(IOException e) {
                creator().openPrivateChannel().flatMap(channel -> channel
                    .sendMessage("Settings file is broken.")
                    .addFile(file)
                ).queue();
            }
        } else {
            LOG.debug("Bot settings file does not exist.");
        }
    }

    private void read(InputStream file, Map<String, Object> map) throws IOException {
        LOG.debug("Reading settings file.");
        map.putAll(mapper.readValue(file, REF_MAP));
    }

    public void save() {
        try {
            mapper.writeValue(file, map);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void put(String key, Object value) {
        var prev = map.get(key);
        if(
            prev == null ||
            Utilities.getClass(value).isAssignableFrom(Utilities.getClass(prev))
        ) {
            map.put(key, value);
            save();
        } else {
            var cprev = Utilities.getClass(prev);
            var cval = Utilities.getClass(value);

            throw new IllegalArgumentException("Setting '" + key + "' already exists with the type '" + cprev.getName() + "' and isn't assignable to '" + cval.getName() + "'.");
        }
    }

    public <T> T get(String key, Class<T> type) {
        var prev = map.get(key);
        if(
            prev != null &&
            !type.isAssignableFrom(Utilities.getClass(prev))
        ) {
            var cprev = Utilities.getClass(prev);
            throw new IllegalArgumentException("Setting '" + key + "' already exists with the type '" + cprev.getName() + "' and isn't assignable to '" + type.getName() + "'.");
        } else {
            return (T)prev;
        }
    }

    public Object get(String key) {
        return map.get(key);
    }

    public Object remove(String key) {
        var obj = map.remove(key);
        save();

        return obj;
    }

    public boolean has(String key) {
        return map.containsKey(key);
    }

    public File getFile() {
        return file;
    }
}
