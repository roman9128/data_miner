package rt.ai;

import rt.storage.DatabaseManager;
import rt.model.ai.QueryContext;
import rt.model.ai.Tool;
import rt.model.db_info.DatabaseStats;

import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseStatsTool implements Tool {

    private final DatabaseManager db;

    public DatabaseStatsTool(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public String getName() {
        return Constants.DATABASE_STATS_TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return Constants.DATABASE_STATS_TOOL_DESC.formatted(Constants.DISTRIBUTION_LIMIT, Constants.DISTRIBUTION_LIMIT);
    }

    @Override
    public String getParameters() {
        return Constants.DATABASE_STATS_TOOL_PARAMS;
    }

    @Override
    public String execute(String arguments) {
        DatabaseStats stats = db.getDatabaseStats();
        return Constants.DATABASE_STATS_TOOL_RESULT.formatted(
                stats.messageCount(),
                stats.chatCount(),
                stats.entityCount(),
                stats.topicCount(),
                stats.activeDayCount(),
                stats.firstMessageAt(),
                stats.lastMessageAt(),
                Constants.DISTRIBUTION_LIMIT,
                formatDistribution(stats.messagesByTopic()),
                Constants.DISTRIBUTION_LIMIT,
                formatDistribution(stats.messagesByEntity())
        );
    }

    private String formatDistribution(Map<String, Long> distribution) {
        return distribution.entrySet().stream()
                .limit(Constants.DISTRIBUTION_LIMIT)
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
    }
}