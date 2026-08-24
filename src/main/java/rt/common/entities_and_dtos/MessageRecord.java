package rt.common.entities_and_dtos;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record MessageRecord(

        long messageId,
        long chatId,
        String chatName,
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

        MessageContentType contentType,
        String text,
        int textLength,
        int wordCount,
        double averageWordLength,
        int emojiCount,

        long replyToChatId,
        long replyToMessageId,
        long forwardOriginChatId,
        long forwardOriginMessageId,

        List<Noun> nouns,
        Set<NamedEntity> namedEntities,
        Map<String, Double> topicConfidenceMap
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageRecord other)) return false;
        return messageId == other.messageId && chatId == other.chatId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, chatId);
    }
}