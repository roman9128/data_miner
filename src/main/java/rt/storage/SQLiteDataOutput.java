package rt.storage;

import rt.model.ai.QueryContext;
import rt.model.document.InfoToShow;
import rt.model.noun.Noun;
import rt.common_utils.Text;
import rt.common_utils.Vector;

import java.sql.*;
import java.util.*;

final class SQLiteDataOutput {

    Map<Long, float[]> getMessageIdsAndEmbeddingsAsMap(QueryContext queryContext) throws SQLException {
        Filter filter = DatabaseUtils.buildMessageFilter(queryContext);
        String sql = """
                SELECT m.id, m.embedding
                FROM messages m
                %s
                """.formatted(
                filter.whereClause().isEmpty()
                        ? "WHERE m.embedding IS NOT NULL"
                        : filter.whereClause() + " AND m.embedding IS NOT NULL"
        );
        Map<Long, float[]> embeddings = new HashMap<>();
        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            DatabaseUtils.setParameters(statement, filter.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    float[] embedding = Vector.byteArrayToFloatArray(resultSet.getBytes("embedding"));
                    embeddings.put(id, embedding);
                }
            }
        }
        return embeddings;
    }

    List<InfoToShow> searchMessagesExact(String query, QueryContext queryContext, int limit) throws SQLException {
        Filter filter = DatabaseUtils.buildMessageFilter(queryContext);
        String whereClause;
        if (filter.whereClause().isEmpty()) {
            whereClause = """
                    WHERE m.text IS NOT NULL
                      AND instr(lower(m.text), lower(?)) > 0
                    """;
        } else {
            whereClause = filter.whereClause() + """
                    AND m.text IS NOT NULL
                    AND instr(lower(m.text), lower(?)) > 0
                    """;
        }
        String sql = """
                SELECT
                    m.text,
                    m.link,
                    m.published_at,
                    m.source_name
                FROM messages m
                %s
                ORDER BY
                     m.publish_year DESC,
                     m.publish_month DESC,
                     m.publish_day DESC,
                     m.publish_hour DESC,
                     m.publish_minute DESC,
                     m.publish_second DESC
                LIMIT %d
                """.formatted(whereClause, limit);
        List<InfoToShow> messages = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            for (Object parameter : filter.parameters()) {
                statement.setObject(parameterIndex++, parameter);
            }
            statement.setString(parameterIndex, query);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(new InfoToShow(
                            resultSet.getString("text"),
                            resultSet.getString("link"),
                            resultSet.getString("published_at"),
                            resultSet.getString("source_name")
                    ));
                }
            }
        }
        return messages;
    }

    List<InfoToShow> searchMessagesByNouns(List<Noun> nouns, QueryContext queryContext, int limit) throws SQLException {

        List<String> lemmas = nouns.stream()
                .map(Noun::lemma)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        if (lemmas.isEmpty()) {
            return List.of();
        }
        Filter filter = DatabaseUtils.buildMessageFilter(queryContext);
        String placeholders = String.join(", ", Collections.nCopies(lemmas.size(), "?"));
        String whereClause;
        if (filter.whereClause().isEmpty()) {
            whereClause = """
                    WHERE mn.lemma IN (%s)
                    """.formatted(placeholders);
        } else {
            whereClause = filter.whereClause() + """
                    AND mn.lemma IN (%s)
                    """.formatted(placeholders);
        }

        String sql = """
                SELECT
                    m.text,
                    m.link,
                    m.published_at,
                    m.source_name
                FROM messages m
                JOIN message_nouns mn_msg
                    ON mn_msg.message_id = m.id
                JOIN nouns mn
                    ON mn.noun_id = mn_msg.noun_id
                %s
                GROUP BY
                    m.id,
                    m.text,
                    m.link,
                    m.published_at,
                    m.source_name
                HAVING COUNT(DISTINCT mn.lemma) = %d
                ORDER BY
                    m.publish_year DESC,
                    m.publish_month DESC,
                    m.publish_day DESC,
                    m.publish_hour DESC,
                    m.publish_minute DESC,
                    m.publish_second DESC
                LIMIT %d
                """.formatted(
                whereClause,
                lemmas.size(),
                limit
        );

        List<InfoToShow> messages = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int parameterIndex = 1;

            for (Object parameter : filter.parameters()) {
                statement.setObject(parameterIndex++, parameter);
            }

            for (String lemma : lemmas) {
                statement.setString(parameterIndex++, lemma);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(new InfoToShow(
                            resultSet.getString("text"),
                            resultSet.getString("link"),
                            resultSet.getString("published_at"),
                            resultSet.getString("source_name")
                    ));
                }
            }
        }
        return messages;
    }

    List<InfoToShow> searchMessagesByTopic(String query, QueryContext queryContext, int limit) throws SQLException {
        Filter filter = DatabaseUtils.buildMessageFilter(queryContext);
        String whereClause;
        if (filter.whereClause().isEmpty()) {
            whereClause = """
                    WHERE lower(t.topic_name) = lower(?)
                    """;
        } else {
            whereClause = filter.whereClause() + """
                    AND lower(t.topic_name) = lower(?)
                    """;
        }

        String sql = """
                SELECT
                    m.text,
                    m.link,
                    m.published_at,
                    m.source_name
                FROM messages m
                JOIN message_topics mt
                    ON mt.message_id = m.id
                JOIN topics t
                    ON t.topic_id = mt.topic_id
                %s
                ORDER BY
                    m.publish_year DESC,
                    m.publish_month DESC,
                    m.publish_day DESC,
                    m.publish_hour DESC,
                    m.publish_minute DESC,
                    m.publish_second DESC
                LIMIT %d
                """.formatted(whereClause, limit);

        List<InfoToShow> messages = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int parameterIndex = 1;

            for (Object parameter : filter.parameters()) {
                statement.setObject(parameterIndex++, parameter);
            }

            statement.setString(parameterIndex, query);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    messages.add(new InfoToShow(
                            resultSet.getString("text"),
                            resultSet.getString("link"),
                            resultSet.getString("published_at"),
                            resultSet.getString("source_name")
                    ));
                }
            }
        }

        return messages;

    }

    List<InfoToShow> getLastMessages(QueryContext queryContext, int limit) throws SQLException {
        if (limit <= 0) return List.of();

        Filter filter = DatabaseUtils.buildMessageFilter(queryContext);
        String whereClause = filter.whereClause();
        String sql = """
                SELECT
                    m.text,
                    m.link,
                    m.published_at,
                    m.source_name
                FROM messages m
                %s
                ORDER BY
                     m.publish_year DESC,
                     m.publish_month DESC,
                     m.publish_day DESC,
                     m.publish_hour DESC,
                     m.publish_minute DESC,
                     m.publish_second DESC
                LIMIT %d
                """.formatted(whereClause, limit);
        List<InfoToShow> messages = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            for (Object parameter : filter.parameters()) {
                statement.setObject(parameterIndex++, parameter);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(new InfoToShow(
                            resultSet.getString("text"),
                            resultSet.getString("link"),
                            resultSet.getString("published_at"),
                            resultSet.getString("source_name")
                    ));
                }
            }
        }
        return messages;
    }

    List<InfoToShow> getMessagesByIds(List<Long> messageIds) throws SQLException {
        if (messageIds == null || messageIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<InfoToShow> messages = new ArrayList<>();
        int batchSize = 900;

        for (int i = 0; i < messageIds.size(); i += batchSize) {
            List<Long> batch = messageIds.subList(i, Math.min(i + batchSize, messageIds.size()));
            messages.addAll(getMessagesByIdsBatch(batch));
        }
        return messages;
    }

    private List<InfoToShow> getMessagesByIdsBatch(List<Long> messageIds) throws SQLException {
        String placeholders = String.join(",", Collections.nCopies(messageIds.size(), "?"));
        String sql = "SELECT text, link, published_at, source_name FROM messages WHERE id IN (" + placeholders + ")";

        List<InfoToShow> messages = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            for (int i = 0; i < messageIds.size(); i++) {
                preparedStatement.setLong(i + 1, messageIds.get(i));
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(new InfoToShow(
                            resultSet.getString("text"),
                            resultSet.getString("link"),
                            resultSet.getString("published_at"),
                            resultSet.getString("source_name")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Ошибка получения сообщений по списку id", e);
        }
        return messages;
    }

    List<Long> findEntityIds(String query) throws SQLException {
        String pattern = "%" + Text.escapeLikePattern(query) + "%";

        String sql = """
                SELECT DISTINCT ne.entity_id
                FROM named_entities ne
                WHERE ne.entity_name LIKE ? ESCAPE '\\'
                   OR ne.category LIKE ? ESCAPE '\\'
                   OR ne.description LIKE ? ESCAPE '\\'
                   OR EXISTS (
                        SELECT 1
                        FROM named_entity_synonyms nes
                        WHERE nes.entity_id = ne.entity_id
                          AND nes.synonym LIKE ? ESCAPE '\\'
                   )
                   OR EXISTS (
                        SELECT 1
                        FROM named_entity_tags net
                        WHERE net.entity_id = ne.entity_id
                          AND net.tag LIKE ? ESCAPE '\\'
                   )
                ORDER BY ne.entity_id
                """;

        List<Long> entityIds = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 1; i <= 5; i++) {
                statement.setString(i, pattern);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entityIds.add(resultSet.getLong("entity_id"));
                }
            }
        }
        return entityIds;
    }

    List<Long> findMessageIdsByEntityIds(List<Long> entityIds, QueryContext queryContext, int limit) throws SQLException {
        if (entityIds == null || entityIds.isEmpty()) return List.of();

        Filter filter = DatabaseUtils.buildMessageFilter(queryContext);

        String placeholders = String.join(",", Collections.nCopies(entityIds.size(), "?"));

        String entityCondition = """
                EXISTS (
                    SELECT 1
                    FROM message_entities me_query
                    WHERE me_query.message_id = m.id
                      AND me_query.entity_id IN (%s)
                )
                """.formatted(placeholders);

        String whereClause;

        if (filter.whereClause().isEmpty()) {
            whereClause = " WHERE " + entityCondition;
        } else {
            whereClause = filter.whereClause() + " AND " + entityCondition;
        }

        String sql = """
                SELECT DISTINCT m.id
                FROM messages m
                %s
                ORDER BY
                    m.publish_year DESC,
                    m.publish_month DESC,
                    m.publish_day DESC,
                    m.publish_hour DESC,
                    m.publish_minute DESC,
                    m.publish_second DESC,
                    m.id DESC
                LIMIT %d
                """.formatted(whereClause, limit);

        List<Long> messageIds = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int parameterIndex = 1;

            for (Object parameter : filter.parameters()) {
                statement.setObject(parameterIndex++, parameter);
            }

            for (Long entityId : entityIds) {
                statement.setLong(parameterIndex++, entityId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messageIds.add(resultSet.getLong("id"));
                }
            }
        }
        return messageIds;
    }
}