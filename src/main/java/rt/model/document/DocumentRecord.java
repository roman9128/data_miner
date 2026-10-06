package rt.model.document;

import rt.model.ne.NamedEntity;
import rt.model.noun.Noun;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record DocumentRecord(

        String sourceDocumentId,
        String sourceId,
        String sourceName,
        String link,
        LocalDateTime parsedAt,
        LocalDateTime publishedAt,
        int publishYear,
        Month publishMonth,
        int publishDayOfMonth,
        DayOfWeek publishDayOfWeek,
        int publishHour,
        int publishMinute,
        int publishSecond,
        String contentType,
        String contentSource,

        String text,
        int textLength,
        int wordCount,
        int emojiCount,
        List<Noun> nouns,
        Set<NamedEntity> namedEntities,
        Set<String> topics,
        float[] embedding
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentRecord other)) return false;
        return Objects.equals(contentSource, other.contentSource)
                && Objects.equals(sourceDocumentId, other.sourceDocumentId)
                && Objects.equals(sourceId, other.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentSource, sourceDocumentId, sourceId);
    }
}