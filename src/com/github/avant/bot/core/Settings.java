package com.github.avant.bot.core;

import com.github.avant.bot.utils.*;

import org.slf4j.*;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;

import static com.github.avant.bot.AvantBot.*;

@SuppressWarnings("unchecked")
public class Settings {
    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

    public static final TypeReference<Map<String, Object>> REF_MAP = new TypeReference<>() {};

    private ObjectMapper mapper;
    private File file;
    private Map<String, Object> map;

    public Settings() {
        LOG.debug("Initializing bot settings.");

        mapper = new ObjectMapper();
        file = new File(ROOT_DIR.getAbsolutePath(), "settings.json");
        map = new LinkedHashMap<>();

        setDefaults();

        LOG.debug("Initialized bot settings.");
    }

    private void setDefaults() {
        read();

        if(!has("prefix")) {
            put("prefix", "!");
        }
        if(!has("channels")) {
            Map<String, String> channels = new LinkedHashMap<>();
            channels.put("moderation", "785671460408655922");

            put("channels", channels);
        }
        if(!has("warns")) {
            put("warns", new LinkedHashMap<String, Map<String, Map<String, Object>>>());
        }
    }

    private void read() {
        if(file.exists()) {
            LOG.debug("Found bot settings file; trying to read and parse.");
            try {
                map.putAll(mapper.readValue(file, REF_MAP));
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            LOG.debug("Bot settings file does not exist; automatically setting up default values.");
        }
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
}
