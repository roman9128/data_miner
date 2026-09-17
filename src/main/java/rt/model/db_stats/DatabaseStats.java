package rt.model.db_stats;

import java.time.LocalDateTime;
import java.util.Map;

public record DatabaseStats(
        long messageCount,
        long chatCount,
        long entityCount,
        long topicCount,
        long activeDayCount,
        LocalDateTime firstMessageAt,
        LocalDateTime lastMessageAt,
        Map<String, Long> messagesByTopic,
        Map<String, Long> messagesByEntity
) {
}