package rt.utils;

import java.util.Locale;

public final class TextUtils {

    private TextUtils() {
    }

    public static String[] getWordsFrom(String text) {
        if (text == null || text.isEmpty()) return new String[0];
        text = text
                .toLowerCase(Locale.ROOT)
                .replaceAll("ё", "е")
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.isEmpty()) return new String[0];
        return text.split("\\s+");
    }

    public static String normalize(String text) {
        return text
                .toLowerCase(Locale.ROOT)
                .replaceAll("ё", "е")
                .replaceAll("[^\\p{L}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
