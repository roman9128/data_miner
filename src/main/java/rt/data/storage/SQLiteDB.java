package rt.data.storage;

import rt.common.Notifier;
import rt.common.entities_and_dtos.MessageRecord;
import rt.common.entities_and_dtos.NamedEntity;
import rt.common.entities_and_dtos.Notification;
import rt.common.entities_and_dtos.Noun;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SQLiteDB {

    private final SQLiteConnector connector;

    public SQLiteDB() {
        this.connector = new SQLiteConnector();
        checkDbFolder();
        connector.createTables();
    }

    public void createRecord(MessageRecord messageRecord) {
        connector.addRecord(messageRecord);
    }

    public void exportToCsv() {
        connector.exportToCsv();
    }

    private void checkDbFolder() {
        final Path dbFolder = Path.of("db");

        if (!Files.exists(dbFolder)) {
            try {
                Files.createDirectories(dbFolder);
            } catch (IOException e) {
                System.err.println("Ошибка при создании папки для базы данных: " + e);
            }
        }
    }

    private class SQLiteConnector {

        private final String DB_URL = "jdbc:sqlite:./db/records.db";
        private final Path exportDirectory = Path.of("dataset");

        private void createTables() {

            String createMessagesTable = """
                    CREATE TABLE IF NOT EXISTS messages (
                        id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    
                        telegram_message_id INTEGER NOT NULL,
                        telegram_chat_id    INTEGER NOT NULL,
                    
                        chat_name           TEXT,
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
                        text                TEXT,
                        text_length         INTEGER NOT NULL,
                        word_count          INTEGER NOT NULL,
                        average_word_length REAL NOT NULL,
                        emoji_count         INTEGER NOT NULL,
                    
                        reply_to_chat_id          INTEGER NOT NULL,
                        reply_to_message_id       INTEGER NOT NULL,
                        forward_origin_chat_id    INTEGER NOT NULL,
                        forward_origin_message_id INTEGER NOT NULL,
                    
                        UNIQUE (
                            telegram_message_id,
                            telegram_chat_id
                        )
                    )
                    """;

            String createNounsTable = """
                    CREATE TABLE IF NOT EXISTS nouns (
                        noun_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    
                        lemma   TEXT NOT NULL,
                    
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
                    
                        FOREIGN KEY (message_id)
                            REFERENCES messages(id),
                    
                        FOREIGN KEY (noun_id)
                            REFERENCES nouns(noun_id)
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
                    
                        FOREIGN KEY (entity_id)
                            REFERENCES named_entities(entity_id)
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
                    
                        FOREIGN KEY (entity_id)
                            REFERENCES named_entities(entity_id)
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
                    
                        FOREIGN KEY (message_id)
                            REFERENCES messages(id),
                    
                        FOREIGN KEY (entity_id)
                            REFERENCES named_entities(entity_id)
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
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    
                        message_id  INTEGER NOT NULL,
                        topic_id    INTEGER NOT NULL,
                    
                        confidence  REAL NOT NULL,
                    
                        UNIQUE (
                            message_id,
                            topic_id
                        ),
                    
                        FOREIGN KEY (message_id)
                            REFERENCES messages(id),
                    
                        FOREIGN KEY (topic_id)
                            REFERENCES topics(topic_id)
                    )
                    """;

            try (Connection connection = DriverManager.getConnection(DB_URL);
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
                throw new RuntimeException(
                        "Ошибка создания таблиц SQLite",
                        e
                );
            }
        }

        private void addRecord(MessageRecord messageRecord) {

            try (Connection connection = DriverManager.getConnection(DB_URL)) {

                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA busy_timeout = 5000");
                    statement.execute("PRAGMA foreign_keys = ON");
                }

                connection.setAutoCommit(false);

                try {
                    long databaseMessageId = saveMessage(connection, messageRecord);
                    deleteMessageRelations(connection, databaseMessageId);
                    saveNouns(connection, databaseMessageId, messageRecord.nouns());
                    saveNamedEntities(connection, databaseMessageId, messageRecord.namedEntities());
                    saveTopics(connection, databaseMessageId, messageRecord.topicConfidenceMap());

                    connection.commit();
                } catch (Exception e) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackException) {
                        e.addSuppressed(rollbackException);
                    }
                    Notifier.instance().add(Notification.Level.SHOW_USER,
                            "Ошибка сохранения сообщения: " +
                                    messageRecord.messageId() +
                                    ", chatId=" +
                                    messageRecord.chatId() + ": " + e);
                }
            } catch (SQLException e) {
                Notifier.instance().add(
                        Notification.Level.SHOW_USER,
                        "Ошибка подключения к SQLite: " + e
                );
            }
        }

        private long saveMessage(Connection connection, MessageRecord messageRecord) throws SQLException {

            String sql = """
                    INSERT INTO messages (
                        telegram_message_id,
                        telegram_chat_id,
                        chat_name,
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
                        text,
                        text_length,
                        word_count,
                        average_word_length,
                        emoji_count,
                        reply_to_chat_id,
                        reply_to_message_id,
                        forward_origin_chat_id,
                        forward_origin_message_id
                    )
                    VALUES (
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                    )
                    ON CONFLICT (
                        telegram_message_id,
                        telegram_chat_id
                    )
                    DO UPDATE SET
                        chat_name = excluded.chat_name,
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
                        average_word_length = excluded.average_word_length,
                        emoji_count = excluded.emoji_count,
                        reply_to_chat_id = excluded.reply_to_chat_id,
                        reply_to_message_id = excluded.reply_to_message_id,
                        forward_origin_chat_id = excluded.forward_origin_chat_id,
                        forward_origin_message_id = excluded.forward_origin_message_id
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                int i = 1;

                statement.setLong(i++, messageRecord.messageId());
                statement.setLong(i++, messageRecord.chatId());
                statement.setString(i++, messageRecord.chatName());
                statement.setString(i++, messageRecord.link());
                statement.setString(i++, messageRecord.parsedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
                statement.setString(i++, messageRecord.publishedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
                statement.setInt(i++, messageRecord.publishYear());
                statement.setInt(i++, messageRecord.publishMonth().getValue());
                statement.setInt(i++, messageRecord.publishDayOfMonth());
                statement.setInt(i++, messageRecord.publishDayOfWeek().getValue());
                statement.setInt(i++, messageRecord.publishHour());
                statement.setInt(i++, messageRecord.publishMinute());
                statement.setInt(i++, messageRecord.publishSecond());
                statement.setString(i++, messageRecord.contentType().name());
                statement.setString(i++, messageRecord.text());
                statement.setInt(i++, messageRecord.textLength());
                statement.setInt(i++, messageRecord.wordCount());
                statement.setDouble(i++, messageRecord.averageWordLength());
                statement.setInt(i++, messageRecord.emojiCount());
                statement.setLong(i++, messageRecord.replyToChatId());
                statement.setLong(i++, messageRecord.replyToMessageId());
                statement.setLong(i++, messageRecord.forwardOriginChatId());
                statement.setLong(i++, messageRecord.forwardOriginMessageId());

                statement.executeUpdate();
            }

            String selectId = """
                    SELECT id
                    FROM messages
                    WHERE telegram_message_id = ?
                      AND telegram_chat_id = ?
                    """;

            try (PreparedStatement statement = connection.prepareStatement(selectId)) {

                statement.setLong(1, messageRecord.messageId());
                statement.setLong(2, messageRecord.chatId());

                try (ResultSet resultSet = statement.executeQuery()) {

                    if (!resultSet.next()) {
                        throw new SQLException("Сообщение не найдено после сохранения: " + messageRecord.messageId());
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

            try (
                    PreparedStatement insertNounStatement = connection.prepareStatement(insertNoun);
                    PreparedStatement selectNounIdStatement = connection.prepareStatement(selectNounId);
                    PreparedStatement insertMessageNounStatement = connection.prepareStatement(insertMessageNoun)
            ) {

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

        private void saveTopics(Connection connection, long messageId, Map<String, Double> topics) throws SQLException {

            if (topics == null || topics.isEmpty()) {
                return;
            }

            String insertTopic = """
                    INSERT OR IGNORE INTO topics (
                        topic_name
                    )
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
                        topic_id,
                        confidence
                    )
                    VALUES (?, ?, ?)
                    """;

            try (
                    PreparedStatement insertTopicStatement = connection.prepareStatement(insertTopic);
                    PreparedStatement selectTopicIdStatement = connection.prepareStatement(selectTopicId);
                    PreparedStatement insertMessageTopicStatement = connection.prepareStatement(insertMessageTopic)
            ) {

                for (Map.Entry<String, Double> entry : topics.entrySet()) {
                    String topic = entry.getKey();
                    if (topic == null || topic.isBlank()) {
                        continue;
                    }
                    Double confidence = entry.getValue();
                    if (confidence == null) {
                        continue;
                    }
                    insertTopicStatement.setString(1, topic);
                    insertTopicStatement.executeUpdate();

                    long topicId;

                    selectTopicIdStatement.setString(1, topic);

                    try (ResultSet resultSet = selectTopicIdStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Не удалось получить ID темы: " + topic);
                        }
                        topicId = resultSet.getLong("topic_id");
                    }
                    insertMessageTopicStatement.setLong(1, messageId);
                    insertMessageTopicStatement.setLong(2, topicId);
                    insertMessageTopicStatement.setDouble(3, confidence);
                    insertMessageTopicStatement.executeUpdate();
                }
            }
        }

        private void exportToCsv() {
            try {
                Files.createDirectories(exportDirectory);
            } catch (IOException e) {
                Notifier.instance().add(Notification.Level.SHOW_USER, "Не удалось создать директорию dataset " + e);
                return;
            }

            String[] tables = {
                    "messages",
                    "nouns",
                    "message_nouns",
                    "named_entities",
                    "named_entity_synonyms",
                    "named_entity_tags",
                    "message_entities",
                    "topics",
                    "message_topics"
            };
            try (Connection connection = DriverManager.getConnection(DB_URL)) {
                for (String table : tables) {
                    exportTableToCsv(connection, table, exportDirectory);
                }
            } catch (Exception e) {
                Notifier.instance().add(Notification.Level.SHOW_USER, "Ошибка экспорта SQLite в CSV " + e);
            }
        }

        private void exportTableToCsv(Connection connection, String tableName, Path exportDirectory) throws SQLException, IOException {
            Path file = exportDirectory.resolve(tableName + ".csv");
            String sql = "SELECT * FROM " + tableName;
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql);
                 BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                ResultSetMetaData metadata = resultSet.getMetaData();
                int columnCount = metadata.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) {
                        writer.write(";");
                    }
                    writer.write(csvEscape(metadata.getColumnName(i)));
                }
                writer.newLine();
                while (resultSet.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) {
                            writer.write(";");
                        }
                        Object value = resultSet.getObject(i);
                        if (value != null) {
                            writer.write(csvEscape(value.toString()));
                        }
                    }
                    writer.newLine();
                }
            }
        }

        private String csvEscape(String value) {
            if (value == null) {
                return "";
            }
            String escaped = value.replace("\"", "\"\"");
            if (escaped.contains(";") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
                return "\"" + escaped + "\"";
            }
            return escaped;
        }
    }
}