package rt.model.document;

public record TextStatistics(
        int wordCount,
        double averageWordLength,
        int emojiCount
) {
}
