package rt.model.message;

public record TextStatistics(
        int wordCount,
        double averageWordLength,
        int emojiCount
) {
}
