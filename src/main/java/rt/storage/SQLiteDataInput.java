package rt.storage;

import rt.model.document.DocumentRecord;
import rt.model.ne.NamedEntity;
import rt.model.noun.Noun;
import rt.common_utils.DateTime;
import rt.common_utils.Vector;

import java.sql.*;
import java.util.List;
import java.util.Set;

final class SQLiteDataInput {

    void addRecord(DocumentRecord documentRecord) throws SQLException {

        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL)) {

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA busy_timeout = 5000");
                statement.execute("PRAGMA foreign_keys = ON");
            }

            connection.setAutoCommit(false);

            try {
                long databaseMessageId = saveMessage(connection, documentRecord);
                deleteMessageRelations(connection, databaseMessageId);
                saveNouns(connection, databaseMessageId, documentRecord.nouns());
                saveNamedEntities(connection, databaseMessageId, documentRecord.namedEntities());
                saveTopics(connection, databaseMessageId, documentRecord.topics());

                connection.commit();
            } catch (Exception e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw new SQLException(
                        "Ошибка сохранения сообщения: " +
                                documentRecord.sourceDocumentId() +
                                ", sourceId=" +
                                documentRecord.sourceId() + ": " + e);
            }
        } catch (SQLException e) {
            throw new SQLException("Ошибка подключения к SQLite: " + e);
        }
    }

    private long saveMessage(Connection connection, DocumentRecord documentRecord) throws SQLException {

        String sql = """
                INSERT INTO messages (
                    source_message_id,
                    source_id,
                    source_name,
                    link,
                    parsed_at,
                
                    published_at,
                    publish_year,
                    publish_month,
                    publish_day,
                    publish_day_of_week,
                    publish_hour,
                    publish_minute,
                    publish_second,
                
                    content_type,
                    content_source,
                    text,
                    text_length,
                    word_count,
                    emoji_count,
                
                    embedding
                )
                VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?,
                    ?
                )
                ON CONFLICT (
                    content_source,
                    source_message_id,
                    source_id
                )
                DO UPDATE SET
                    source_name = excluded.source_name,
                    link = excluded.link,
                    parsed_at = excluded.parsed_at,
                
                    published_at = excluded.published_at,
                    publish_year = excluded.publish_year,
                    publish_month = excluded.publish_month,
                    publish_day = excluded.publish_day,
                    publish_day_of_week = excluded.publish_day_of_week,
                    publish_hour = excluded.publish_hour,
                    publish_minute = excluded.publish_minute,
                    publish_second = excluded.publish_second,
                
                    content_type = excluded.content_type,
                    text = excluded.text,
                    text_length = excluded.text_length,
                    word_count = excluded.word_count,
                    emoji_count = excluded.emoji_count,
                
                    embedding = excluded.embedding
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;

            statement.setString(i++, documentRecord.sourceDocumentId());
            statement.setString(i++, documentRecord.sourceId());
            statement.setString(i++, documentRecord.sourceName());
            statement.setString(i++, documentRecord.link());
            statement.setString(i++, DateTime.getStringOf(documentRecord.parsedAt()));
            statement.setString(i++, DateTime.getStringOf(documentRecord.publishedAt()));
            statement.setInt(i++, documentRecord.publishYear());
            statement.setInt(i++, documentRecord.publishMonth().getValue());
            statement.setInt(i++, documentRecord.publishDayOfMonth());
            statement.setInt(i++, documentRecord.publishDayOfWeek().getValue());
            statement.setInt(i++, documentRecord.publishHour());
            statement.setInt(i++, documentRecord.publishMinute());
            statement.setInt(i++, documentRecord.publishSecond());
            statement.setString(i++, documentRecord.contentType());
            statement.setString(i++, documentRecord.contentSource());
            statement.setString(i++, documentRecord.text());
            statement.setInt(i++, documentRecord.textLength());
            statement.setInt(i++, documentRecord.wordCount());
            statement.setInt(i++, documentRecord.emojiCount());

            float[] vector = documentRecord.embedding();
            if (vector != null && vector.length > 0) {
                statement.setBytes(i++, Vector.floatArrayToByteArray(vector));
            } else {
                statement.setBytes(i++, null);
            }

            statement.executeUpdate();
        }

        String selectId = """
                SELECT id
                FROM messages
                WHERE source_message_id = ?
                  AND source_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(selectId)) {

            statement.setString(1, documentRecord.sourceDocumentId());
            statement.setString(2, documentRecord.sourceId());

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new SQLException("Сообщение не найдено после сохранения: " + documentRecord.sourceDocumentId());
                }

                return resultSet.getLong("id");
            }
        }
    }

    private void deleteMessageRelations(Connection connection, long messageId) throws SQLException {

        String deleteMessageNouns = """
                DELETE FROM message_nouns
                WHERE message_id = ?
                """;

        String deleteMessageEntities = """
                DELETE FROM message_entities
                WHERE message_id = ?
                """;

        String deleteMessageTopics = """
                DELETE FROM message_topics
                WHERE message_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(deleteMessageNouns)) {
            statement.setLong(1, messageId);
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement(deleteMessageEntities)) {
            statement.setLong(1, messageId);
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement(deleteMessageTopics)) {
            statement.setLong(1, messageId);
            statement.executeUpdate();
        }
    }

    private void saveNouns(Connection connection, long messageId, List<Noun> nouns) throws SQLException {

        if (nouns == null || nouns.isEmpty()) {
            return;
        }

        String insertNoun = """
                INSERT OR IGNORE INTO nouns (
                    lemma
                )
                VALUES (?)
                """;

        String selectNounId = """
                SELECT noun_id
                FROM nouns
                WHERE lemma = ?
                """;

        String insertMessageNoun = """
                INSERT INTO message_nouns (
                    message_id,
                    noun_id,
                    count
                )
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement insertNounStatement = connection.prepareStatement(insertNoun);
             PreparedStatement selectNounIdStatement = connection.prepareStatement(selectNounId);
             PreparedStatement insertMessageNounStatement = connection.prepareStatement(insertMessageNoun)) {

            for (Noun noun : nouns) {

                if (noun == null) {
                    continue;
                }

                String lemma = noun.lemma();
                if (lemma == null || lemma.isBlank()) {
                    continue;
                }

                insertNounStatement.setString(1, lemma);
                insertNounStatement.executeUpdate();

                long nounId;
                selectNounIdStatement.setString(1, lemma);

                try (ResultSet resultSet = selectNounIdStatement.executeQuery()) {

                    if (!resultSet.next()) {
                        throw new SQLException("Не удалось получить ID леммы: " + lemma);
                    }

                    nounId = resultSet.getLong("noun_id");
                }

                insertMessageNounStatement.setLong(1, messageId);
                insertMessageNounStatement.setLong(2, nounId);
                insertMessageNounStatement.setInt(3, noun.count());
                insertMessageNounStatement.executeUpdate();
            }
        }
    }

    private void saveNamedEntities(Connection connection, long messageId, Set<NamedEntity> entities) throws SQLException {

        if (entities == null || entities.isEmpty()) {
            return;
        }

        String insertEntity = """
                INSERT INTO named_entities (
                    entity_name,
                    category,
                    description
                )
                VALUES (?, ?, ?)
                ON CONFLICT (
                    entity_name,
                    category
                )
                DO UPDATE SET
                    description = excluded.description
                """;

        String selectEntityId = """
                SELECT entity_id
                FROM named_entities
                WHERE entity_name = ?
                  AND category = ?
                """;

        String insertMessageEntity = """
                INSERT INTO message_entities (
                    message_id,
                    entity_id
                )
                VALUES (?, ?)
                """;

        String insertSynonym = """
                INSERT OR IGNORE INTO named_entity_synonyms (
                    entity_id,
                    synonym
                )
                VALUES (?, ?)
                """;

        String insertTag = """
                INSERT OR IGNORE INTO named_entity_tags (
                    entity_id,
                    tag
                )
                VALUES (?, ?)
                """;

        try (
                PreparedStatement insertEntityStatement = connection.prepareStatement(insertEntity);
                PreparedStatement selectEntityIdStatement = connection.prepareStatement(selectEntityId);
                PreparedStatement insertMessageEntityStatement = connection.prepareStatement(insertMessageEntity);
                PreparedStatement insertSynonymStatement = connection.prepareStatement(insertSynonym);
                PreparedStatement insertTagStatement = connection.prepareStatement(insertTag)
        ) {

            for (NamedEntity entity : entities) {

                if (entity == null) {
                    continue;
                }

                String name = entity.getName();

                if (name == null || name.isBlank()) {
                    continue;
                }

                String category = entity.getCategory();

                if (category == null || category.isBlank()) {
                    category = "UNKNOWN";
                }

                insertEntityStatement.setString(1, name);
                insertEntityStatement.setString(2, category);
                insertEntityStatement.setString(3, entity.getDescription());
                insertEntityStatement.executeUpdate();

                long entityId;

                selectEntityIdStatement.setString(1, name);
                selectEntityIdStatement.setString(2, category);

                try (ResultSet resultSet = selectEntityIdStatement.executeQuery()) {

                    if (!resultSet.next()) {
                        throw new SQLException("Не удалось получить ID NamedEntity: " + name);
                    }

                    entityId = resultSet.getLong("entity_id");
                }

                insertMessageEntityStatement.setLong(1, messageId);
                insertMessageEntityStatement.setLong(2, entityId);
                insertMessageEntityStatement.executeUpdate();

                if (entity.getSynonyms() != null) {
                    for (String synonym : entity.getSynonyms()) {
                        if (synonym == null || synonym.isBlank()) {
                            continue;
                        }
                        insertSynonymStatement.setLong(1, entityId);
                        insertSynonymStatement.setString(2, synonym);
                        insertSynonymStatement.executeUpdate();
                    }
                }

                if (entity.getTags() != null) {
                    for (String tag : entity.getTags()) {
                        if (tag == null || tag.isBlank()) {
                            continue;
                        }
                        insertTagStatement.setLong(1, entityId);
                        insertTagStatement.setString(2, tag);
                        insertTagStatement.executeUpdate();
                    }
                }
            }
        }
    }

    private void saveTopics(Connection connection, long messageId, Set<String> topics) throws SQLException {
        if (topics == null || topics.isEmpty()) return;

        String insertTopic = """
                INSERT OR IGNORE INTO topics (topic_name)
                VALUES (?)
                """;

        String selectTopicId = """
                SELECT topic_id
                FROM topics
                WHERE topic_name = ?
                """;

        String insertMessageTopic = """
                INSERT INTO message_topics (
                    message_id,
                    topic_id
                )
                VALUES (?, ?)
                """;

        try (
                PreparedStatement insertTopicStatement = connection.prepareStatement(insertTopic);
                PreparedStatement selectTopicIdStatement = connection.prepareStatement(selectTopicId);
                PreparedStatement insertMessageTopicStatement = connection.prepareStatement(insertMessageTopic)
        ) {

            for (String topic : topics) {
                if (topic == null || topic.isBlank()) continue;

                insertTopicStatement.setString(1, topic);
                insertTopicStatement.executeUpdate();
                selectTopicIdStatement.setString(1, topic);

                long topicId;

                try (ResultSet resultSet = selectTopicIdStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Не удалось получить ID темы: " + topic);
                    }
                    topicId = resultSet.getLong("topic_id");
                }
                insertMessageTopicStatement.setLong(1, messageId);
                insertMessageTopicStatement.setLong(2, topicId);
                insertMessageTopicStatement.executeUpdate();
            }
        }
    }
}