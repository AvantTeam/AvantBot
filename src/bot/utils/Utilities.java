package bot.utils;

public final class Utilities {
    private Utilities() {}

    public static Class<?> getClass(Object object) {
        Class<?> type = object.getClass();
        while(type.isAnonymousClass()) {
            type = type.getSuperclass();
        }

        return type;
    }
}
