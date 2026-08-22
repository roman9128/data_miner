package rt.common.entities_and_dtos;

public record TextStatistics(
        int wordCount,
        double averageWordLength,
        int emojiCount
) {
}
