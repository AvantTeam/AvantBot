package com.github.avant.bot.utils;

public final class ElementUtils {
    private ElementUtils() {}

    public static Class<?> getClass(Object object) {
        Class<?> type = object.getClass();
        while(type.isAnonymousClass()) {
            type = type.getSuperclass();
        }

        return type;
    }
}
