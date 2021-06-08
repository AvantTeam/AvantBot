package bot.utils;

import java.util.*;

public final class Utilities {
    private Utilities() {}

    /**
     * @param object The object.
     * @return the superclass if this object if its class is anonymous.
     */
    public static Class<?> getClass(Object object) {
        Class<?> type = object.getClass();
        while(type.isAnonymousClass()) {
            type = type.getSuperclass();
        }

        return type;
    }

    /**
     * Recursively removes null keys from this map.
     * @param map The map whose keys are to be filtered out.
     */
    public static void ensureNulls(Map<?, ?> map) {
        Iterator<?> it = map.keySet().iterator();
        while(it.hasNext()) {
            Object key = it.next();
            if(key == null) {
                it.remove();
            } else if(map.get(key) instanceof Map<?, ?> child) {
                ensureNulls(child);
            }
        }
    }
}
