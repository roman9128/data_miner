package rt.storage;

import rt.core.Notifier;
import rt.model.ai.DatabaseContext;
import rt.model.ai.QueryContext;
import rt.model.db_info.DatabaseStats;
import rt.model.message.InfoToShow;
import rt.model.message.MessageRecord;
import rt.model.notification.Notification;
import rt.model.noun.Noun;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final SQLiteConnector connector;

    public DatabaseManager() {
        this.connector = new SQLiteConnector();
        checkDbFolder();
        connector.createTables();
    }

    public void createRecord(MessageRecord messageRecord) {
        try {
            connector.addRecord(messageRecord);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
        }
    }

    public void exportToCsv() {
        try {
            connector.exportToCsv();
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
        }
    }

    public Map<Long, float[]> getMessageIdsAndEmbeddings(QueryContext queryContext) {
        try {
            return connector.getMessageIdsAndEmbeddingsAsMap(queryContext);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return Map.of();
        }
    }

    public List<InfoToShow> getMessagesByIds(Collection<Long> ids) {
        try {
            return connector.getMessagesByIds(List.copyOf(ids));
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return List.of();
        }
    }

    public List<InfoToShow> searchMessagesExact(String query, QueryContext queryContext, int limit) {
        if (query == null || query.isBlank() || limit <= 0) return List.of();

        try {
            return connector.searchMessagesExact(query, queryContext, limit);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return List.of();
        }
    }

    public List<InfoToShow> searchMessagesByNouns(List<Noun> nouns, QueryContext queryContext, int limit) {
        if (nouns == null || nouns.isEmpty() || limit <= 0) return List.of();

        try {
            return connector.searchMessagesByNouns(nouns, queryContext, limit);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return List.of();
        }
    }

    public List<InfoToShow> searchMessagesByEntities(String query, QueryContext queryContext, int limit) {
        if (query == null || query.isBlank() || limit <= 0) return List.of();

        try {
            List<Long> entityIds = connector.findEntityIds(query);
            if (entityIds.isEmpty()) return List.of();

            List<Long> messageIds = connector.findMessageIdsByEntityIds(entityIds, queryContext, limit);
            if (messageIds.isEmpty()) return List.of();

            return connector.getMessagesByIds(messageIds);

        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return List.of();
        }
    }

    public List<InfoToShow> getLastMessages(QueryContext queryContext, int limit) {
        try {
            return connector.getLastMessages(queryContext, limit);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return List.of();
        }
    }

    public DatabaseStats getDatabaseStats(QueryContext queryContext) {
        try {
            return connector.getDatabaseStats(queryContext);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return new DatabaseStats(
                    0,
                    0,
                    0,
                    0,
                    0,
                    null,
                    null,
                    Map.of(),
                    Map.of()
            );
        }
    }

    public DatabaseContext getDatabaseContext() {
        try {
            return connector.getDatabaseContext();
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, e.getMessage());
            return new DatabaseContext(null, null, null);
        }
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
}