package rt.data_processing.stats;

import rt.model.message.TextStatistics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextStatisticsCalculator {

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+(?:[-'][\\p{L}\\p{N}]+)*");

    private TextStatisticsCalculator() {
    }

    public static TextStatistics calculate(String text) {
        if (text == null || text.isBlank()) {
            return new TextStatistics(
                    0,
                    0.0,
                    0
            );
        }

        int wordCount = countWords(text);
        double averageWordLength = calculateAverageWordLength(text);
        int emojiCount = countEmojis(text);

        return new TextStatistics(
                wordCount,
                averageWordLength,
                emojiCount
        );
    }

    private static int countWords(String text) {
        return count(WORD_PATTERN, text);
    }

    private static double calculateAverageWordLength(String text) {

        Matcher matcher = WORD_PATTERN.matcher(text);

        int count = 0;
        int totalLength = 0;

        while (matcher.find()) {
            count++;
            totalLength += matcher.group().codePointCount(0, matcher.group().length());
        }

        return count == 0
                ? 0.0
                : (double) totalLength / count;
    }

    private static int count(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static int countEmojis(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (isEmoji(codePoint)) {
                count++;
            }
            i += Character.charCount(codePoint);
        }
        return count;
    }

    private static boolean isEmoji(int codePoint) {
        return (codePoint >= 0x1F300 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || (codePoint >= 0x2300 && codePoint <= 0x23FF);
    }
}
