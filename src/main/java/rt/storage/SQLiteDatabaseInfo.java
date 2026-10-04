package rt.storage;

import rt.model.ai.DatabaseContext;
import rt.model.ai.QueryContext;
import rt.model.db_info.DatabaseStats;
import rt.model.ne.NamedEntity;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

final class SQLiteDatabaseInfo {

    void createTables() {

        String createMessagesTable = """
                CREATE TABLE IF NOT EXISTS messages (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                
                    source_message_id   TEXT NOT NULL,
                    source_id           TEXT NOT NULL,
                    source_name         TEXT,
                    link                TEXT,
                
                    parsed_at           TEXT NOT NULL,
                
                    published_at        TEXT NOT NULL,
                    publish_year        INTEGER NOT NULL,
                    publish_month       INTEGER NOT NULL,
                    publish_day         INTEGER NOT NULL,
                    publish_day_of_week INTEGER NOT NULL,
                    publish_hour        INTEGER NOT NULL,
                    publish_minute      INTEGER NOT NULL,
                    publish_second      INTEGER NOT NULL,
                
                    content_type        TEXT NOT NULL,
                    content_source      TEXT NOT NULL,
                    text                TEXT,
                    text_length         INTEGER NOT NULL,
                    word_count          INTEGER NOT NULL,
                    average_word_length REAL NOT NULL,
                    emoji_count         INTEGER NOT NULL,
                
                    embedding                 BLOB,
                
                    UNIQUE (
                        content_source,
                        source_message_id,
                        source_id
                    )
                )
                """;

        String createNounsTable = """
                CREATE TABLE IF NOT EXISTS nouns (
                    noun_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                    lemma    TEXT NOT NULL,
                    UNIQUE (lemma)
                )
                """;

        String createMessageNounsTable = """
                CREATE TABLE IF NOT EXISTS message_nouns (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    message_id  INTEGER NOT NULL,
                    noun_id     INTEGER NOT NULL,
                    count       INTEGER NOT NULL,
                    UNIQUE (
                        message_id,
                        noun_id
                    ),
                
                    FOREIGN KEY (message_id) REFERENCES messages(id),
                    FOREIGN KEY (noun_id) REFERENCES nouns(noun_id)
                )
                """;

        String createEntitiesTable = """
                CREATE TABLE IF NOT EXISTS named_entities (
                    entity_id          INTEGER PRIMARY KEY AUTOINCREMENT,
                
                    entity_name        TEXT NOT NULL,
                    category    TEXT NOT NULL,
                    description TEXT,
                
                    UNIQUE (
                        entity_name,
                        category
                    )
                )
                """;

        String createEntitySynonymsTable = """
                CREATE TABLE IF NOT EXISTS named_entity_synonyms (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                
                    entity_id   INTEGER NOT NULL,
                    synonym     TEXT NOT NULL,
                
                    UNIQUE (
                        entity_id,
                        synonym
                    ),
                
                    FOREIGN KEY (entity_id) REFERENCES named_entities(entity_id)
                )
                """;

        String createEntityTagsTable = """
                CREATE TABLE IF NOT EXISTS named_entity_tags (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                
                    entity_id   INTEGER NOT NULL,
                    tag         TEXT NOT NULL,
                
                    UNIQUE (
                        entity_id,
                        tag
                    ),
                
                    FOREIGN KEY (entity_id) REFERENCES named_entities(entity_id)
                )
                """;

        String createMessageEntityTable = """
                CREATE TABLE IF NOT EXISTS message_entities (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                
                    message_id  INTEGER NOT NULL,
                    entity_id   INTEGER NOT NULL,
                
                    UNIQUE (
                        message_id,
                        entity_id
                    ),
                
                    FOREIGN KEY (message_id) REFERENCES messages(id),
                    FOREIGN KEY (entity_id) REFERENCES named_entities(entity_id)
                )
                """;

        String createTopicsTable = """
                CREATE TABLE IF NOT EXISTS topics (
                    topic_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                
                    topic_name    TEXT NOT NULL,
                
                    UNIQUE (topic_name)
                )
                """;

        String createMessageTopicsTable = """
                CREATE TABLE IF NOT EXISTS message_topics (
                    message_id INTEGER NOT NULL,
                    topic_id   INTEGER NOT NULL,
                    PRIMARY KEY (message_id, topic_id),
                    FOREIGN KEY (message_id) REFERENCES messages(id),
                    FOREIGN KEY (topic_id) REFERENCES topics(topic_id)
                )
                """;

        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL);
             Statement statement = connection.createStatement()) {

            statement.execute(createMessagesTable);

            statement.execute(createNounsTable);
            statement.execute(createMessageNounsTable);

            statement.execute(createEntitiesTable);
            statement.execute(createEntitySynonymsTable);
            statement.execute(createEntityTagsTable);
            statement.execute(createMessageEntityTable);

            statement.execute(createTopicsTable);
            statement.execute(createMessageTopicsTable);

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания таблиц SQLite", e);
        }
    }

    DatabaseStats getDatabaseStats(QueryContext context) throws SQLException {
        Filter filter = DatabaseUtils.buildMessageFilter(context);
        long messageCount;
        long chatCount;
        long entityCount;
        long topicCount;
        long activeDayCount;
        LocalDateTime firstMessageAt;
        LocalDateTime lastMessageAt;
        Map<String, Long> messagesByTopic;
        Map<String, Long> messagesByEntity;

        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL)) {
            String countsSql = """
                    WITH filtered_messages AS (
                        SELECT m.*
                        FROM messages m
                        %s
                    )
                    SELECT
                        COUNT(*) AS message_count,
                        COUNT(DISTINCT source_id) AS chat_count,
                        (
                            SELECT COUNT(DISTINCT me.entity_id)
                            FROM message_entities me
                            JOIN filtered_messages fm
                                ON fm.id = me.message_id
                        ) AS entity_count,
                        (
                            SELECT COUNT(DISTINCT mt.topic_id)
                            FROM message_topics mt
                            JOIN filtered_messages fm
                                ON fm.id = mt.message_id
                        ) AS topic_count,
                        COUNT(DISTINCT
                            publish_year || '-' ||
                            publish_month || '-' ||
                            publish_day
                        ) AS active_day_count
                    FROM filtered_messages
                    """.formatted(filter.whereClause());

            try (PreparedStatement statement = connection.prepareStatement(countsSql)) {
                DatabaseUtils.setParameters(statement, filter.parameters());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Не удалось получить статистику базы данных");
                    }
                    messageCount = resultSet.getLong("message_count");
                    chatCount = resultSet.getLong("chat_count");
                    entityCount = resultSet.getLong("entity_count");
                    topicCount = resultSet.getLong("topic_count");
                    activeDayCount = resultSet.getLong("active_day_count");
                }
            }

            String firstMessageSql = """
                    SELECT
                        publish_year,
                        publish_month,
                        publish_day,
                        publish_hour,
                        publish_minute,
                        publish_second
                    FROM messages m
                    %s
                    ORDER BY
                        publish_year,
                        publish_month,
                        publish_day,
                        publish_hour,
                        publish_minute,
                        publish_second
                    LIMIT 1
                    """.formatted(filter.whereClause());

            try (PreparedStatement statement = connection.prepareStatement(firstMessageSql)) {
                DatabaseUtils.setParameters(statement, filter.parameters());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        firstMessageAt = LocalDateTime.of(
                                resultSet.getInt("publish_year"),
                                resultSet.getInt("publish_month"),
                                resultSet.getInt("publish_day"),
                                resultSet.getInt("publish_hour"),
                                resultSet.getInt("publish_minute"),
                                resultSet.getInt("publish_second")
                        );
                    } else {
                        firstMessageAt = null;
                    }
                }
            }

            String lastMessageSql = """
                    SELECT
                        publish_year,
                        publish_month,
                        publish_day,
                        publish_hour,
                        publish_minute,
                        publish_second
                    FROM messages m
                    %s
                    ORDER BY
                        publish_year DESC,
                        publish_month DESC,
                        publish_day DESC,
                        publish_hour DESC,
                        publish_minute DESC,
                        publish_second DESC
                    LIMIT 1
                    """.formatted(filter.whereClause());

            try (PreparedStatement statement = connection.prepareStatement(lastMessageSql)) {
                DatabaseUtils.setParameters(statement, filter.parameters());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        lastMessageAt = LocalDateTime.of(
                                resultSet.getInt("publish_year"),
                                resultSet.getInt("publish_month"),
                                resultSet.getInt("publish_day"),
                                resultSet.getInt("publish_hour"),
                                resultSet.getInt("publish_minute"),
                                resultSet.getInt("publish_second")
                        );
                    } else {
                        lastMessageAt = null;
                    }
                }
            }
            messagesByTopic = getMessagesByTopic(connection, filter);
            messagesByEntity = getMessagesByEntity(connection, filter);
        }

        return new DatabaseStats(
                messageCount,
                chatCount,
                entityCount,
                topicCount,
                activeDayCount,
                firstMessageAt,
                lastMessageAt,
                messagesByTopic,
                messagesByEntity
        );
    }

    DatabaseContext getDatabaseContext() throws SQLException {
        Map<Long, String> chats = new HashMap<>();
        Set<NamedEntity> namedEntities = new HashSet<>();
        Set<String> topics = new HashSet<>();

        String chatsSql = """
                SELECT source_id,
                       source_name
                FROM messages
                WHERE source_name IS NOT NULL
                  AND source_name <> ''
                GROUP BY source_id, source_name
                ORDER BY source_name
                """;

        String entitiesSql = """
                SELECT entity_id,
                       entity_name,
                       category,
                       description
                FROM named_entities
                ORDER BY entity_name
                """;

        String synonymsSql = """
                SELECT synonym
                FROM named_entity_synonyms
                WHERE entity_id = ?
                ORDER BY synonym
                """;

        String tagsSql = """
                SELECT tag
                FROM named_entity_tags
                WHERE entity_id = ?
                ORDER BY tag
                """;

        String topicsSql = """
                SELECT topic_name
                FROM topics
                ORDER BY topic_name
                """;

        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL)) {

            try (PreparedStatement statement = connection.prepareStatement(chatsSql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long chatId = resultSet.getLong("source_id");
                    String chatName = resultSet.getString("source_name");
                    chats.put(chatId, chatName);
                }
            }

            try (PreparedStatement entityStatement = connection.prepareStatement(entitiesSql);
                 PreparedStatement synonymStatement = connection.prepareStatement(synonymsSql);
                 PreparedStatement tagStatement = connection.prepareStatement(tagsSql);
                 ResultSet resultSet = entityStatement.executeQuery()) {

                while (resultSet.next()) {
                    long entityId = resultSet.getLong("entity_id");
                    String name = resultSet.getString("entity_name");
                    String category = resultSet.getString("category");
                    String description = resultSet.getString("description");
                    Set<String> synonyms = new HashSet<>();
                    synonymStatement.setLong(1, entityId);
                    try (ResultSet synonymResultSet = synonymStatement.executeQuery()) {
                        while (synonymResultSet.next()) {
                            synonyms.add(synonymResultSet.getString("synonym"));
                        }
                    }
                    Set<String> tags = new HashSet<>();
                    tagStatement.setLong(1, entityId);
                    try (ResultSet tagResultSet = tagStatement.executeQuery()) {
                        while (tagResultSet.next()) {
                            tags.add(tagResultSet.getString("tag"));
                        }
                    }
                    namedEntities.add(new NamedEntity(name, synonyms, category, description, tags));
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(topicsSql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    topics.add(resultSet.getString("topic_name"));
                }
            }
        }
        return new DatabaseContext(chats, namedEntities, topics);
    }

    private Map<String, Long> getMessagesByTopic(Connection connection, Filter filter) throws SQLException {

        String sql = """
                WITH filtered_messages AS (
                    SELECT m.*
                    FROM messages m
                    %s
                )
                SELECT
                    t.topic_name,
                    COUNT(DISTINCT mt.message_id) AS message_count
                FROM filtered_messages fm
                JOIN message_topics mt
                    ON mt.message_id = fm.id
                JOIN topics t
                    ON t.topic_id = mt.topic_id
                GROUP BY t.topic_name
                ORDER BY message_count DESC
                """.formatted(filter.whereClause());

        Map<String, Long> result = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            DatabaseUtils.setParameters(statement, filter.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.put(
                            resultSet.getString("topic_name"),
                            resultSet.getLong("message_count")
                    );
                }
            }
        }
        return result;
    }

    private Map<String, Long> getMessagesByEntity(Connection connection, Filter filter) throws SQLException {

        String sql = """
                WITH filtered_messages AS (
                    SELECT m.*
                    FROM messages m
                    %s
                )
                SELECT
                    ne.entity_name,
                    COUNT(DISTINCT me.message_id) AS message_count
                FROM filtered_messages fm
                JOIN message_entities me
                    ON me.message_id = fm.id
                JOIN named_entities ne
                    ON ne.entity_id = me.entity_id
                GROUP BY ne.entity_name
                ORDER BY message_count DESC
                """.formatted(filter.whereClause());

        Map<String, Long> result = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            DatabaseUtils.setParameters(statement, filter.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    result.put(
                            resultSet.getString("entity_name"),
                            resultSet.getLong("message_count")
                    );
                }
            }
        }
        return result;
    }
}