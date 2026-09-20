package rt.ai;

import rt.storage.SQLiteDB;
import rt.model.ai.QueryContext;
import rt.model.ai.Tool;
import rt.model.db_info.DatabaseStats;

import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseStatsTool implements Tool {

    private static final int DISTRIBUTION_LIMIT = 20;
    private final SQLiteDB db;
    private QueryContext queryContext;

    public DatabaseStatsTool(SQLiteDB db) {
        this.db = db;
    }

    @Override
    public String getName() {
        return "get_database_stats";
    }

    @Override
    public String getDescription() {
        return """
            Get general statistics about the message database without loading the messages themselves.
            Use this tool when the user asks to analyze or inspect the entire database, or when the requested scope may contain too many messages to analyze at once.
            The tool returns the total number of messages, chats, named entities, topics, active days, the date range of stored messages, and the distribution of messages by topics and named entities.
            A message can belong to multiple topics and multiple named entities, so it can be counted in multiple groups.
            The distribution contains the top %d topics and top %d named entities by number of associated messages.
            It does not return message contents.

            IMPORTANT:
            If the user asks to inspect, analyze, summarize, or describe the entire database, use only this tool.
            Do not call search_messages with a query such as "all messages", "all data", "entire database", or equivalent requests.
            After receiving the database statistics, ask the user to narrow the request if they want to analyze message contents.
            Use search_messages only after the user has specified a concrete topic, entity, event, time period, chat, or other meaningful search criteria.

            If the requested scope is too large for a single analysis, ask the user to narrow the request by specifying a topic, time period, chat, or other relevant criteria.
            """.formatted(DISTRIBUTION_LIMIT, DISTRIBUTION_LIMIT);
    }

    @Override
    public String getParameters() {
        return """
                {
                  "type": "object",
                  "properties": {}
                }
                """;
    }

    @Override
    public String execute(String arguments) {

        DatabaseStats stats = db.getDatabaseStats(queryContext);

        return """
                Database statistics:
                Total messages: %d
                Total chats: %d
                Total named entities: %d
                Total topics: %d
                Active days: %d
                First message: %s
                Last message: %s

                Top %d topics by number of associated messages:
                %s

                Top %d named entities by number of associated messages:
                %s

                Note: a message can belong to multiple topics and contain multiple named entities.
                Therefore, one message can be counted in several groups, and the sum of topic/entity counts can exceed the total number of messages.

                The database contains too much data to be analyzed in its entirety in a single request.
                Ask the user to narrow the request by specifying a topic, time period, chat, or other relevant criteria.
                """.formatted(
                stats.messageCount(),
                stats.chatCount(),
                stats.entityCount(),
                stats.topicCount(),
                stats.activeDayCount(),
                stats.firstMessageAt(),
                stats.lastMessageAt(),
                DISTRIBUTION_LIMIT,
                formatDistribution(stats.messagesByTopic()),
                DISTRIBUTION_LIMIT,
                formatDistribution(stats.messagesByEntity())
        );
    }

    private String formatDistribution(Map<String, Long> distribution) {
        return distribution.entrySet().stream()
                .limit(DISTRIBUTION_LIMIT)
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public void setQueryContext(QueryContext queryContext) {
        this.queryContext = queryContext;
    }
}