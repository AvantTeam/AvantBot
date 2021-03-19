package com.github.avant.bot.core;

import com.github.avant.bot.utils.*;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;

@SuppressWarnings("unchecked")
public class Settings {
    private ObjectMapper mapper;
    private File file;
    private Map<String, Object> map;

    private TypeReference<Map<String, Object>> ref = new TypeReference<>() {};

    public Settings() {
        mapper = new ObjectMapper();
        file = new File("settings.json");
        map = new HashMap<>();

        setDefaults();
    }

    private void setDefaults() {
        read();

        if(!has("prefix")) {
            put("prefix", "av!");
        }
    }

    private void read() {
        if(file.exists()) {
            try {
                map.putAll(mapper.readValue(file, ref));
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void save() {
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
            ElementUtils.getClass(prev).isAssignableFrom(ElementUtils.getClass(value))
        ) {
            map.put(key, value);
            save();
        } else {
            var cprev = ElementUtils.getClass(prev);
            var cval = ElementUtils.getClass(value);

            throw new IllegalArgumentException("Setting '" + key + "' already exists with the type '" + cprev.getName() + "' and isn't assignable from '" + cval.getName() + "'.");
        }
    }

    public <T> T get(String key, Class<T> type) {
        var prev = map.get(key);
        if(
            prev != null &&
            !ElementUtils.getClass(prev).isAssignableFrom(type)
        ) {
            var cprev = ElementUtils.getClass(prev);
            throw new IllegalArgumentException("Setting '" + key + "' already exists with the type '" + cprev.getName() + "' and isn't assignable from '" + type.getName() + "'.");
        } else {
            return (T)prev;
        }
    }

    public boolean has(String key) {
        return map.containsKey(key);
    }
}
