package rt.utils;

import opennlp.tools.stemmer.snowball.SnowballStemmer;
import rt.model.message.InfoToShow;

import java.util.List;
import java.util.Locale;

public final class TextUtils {

    private static final SnowballStemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.RUSSIAN);

    private TextUtils() {
    }

    public static String[] getWordsAsLettersAndNumbersFrom(String text) {
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

    public static String[] getWordsAsLettersFrom(String text) {
        if (text == null || text.isEmpty()) return new String[0];
        text = normalize(text);
        return text.split("\\s+");
    }

    public static String normalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text
                .toLowerCase(Locale.ROOT)
                .replaceAll("ё", "е")
                .replaceAll("[^\\p{L}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String stem(String text) {
        if (text == null || text.isBlank()) return "";
        return stemmer.stem(text).toString();
    }

    public static String format(List<InfoToShow> messages) {
        StringBuilder sb = new StringBuilder();
        for (InfoToShow m : messages) {
            sb
                    .append("<").append(System.lineSeparator())
                    .append(m.chatName()).append(System.lineSeparator())
                    .append(m.publishedAt()).append(System.lineSeparator())
                    .append(m.link()).append(System.lineSeparator())
                    .append(m.text()).append(System.lineSeparator())
                    .append(">").append(System.lineSeparator());
        }
        return sb.toString();
    }

    public static String escapeLikePattern(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
