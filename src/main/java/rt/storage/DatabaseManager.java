package rt.storage;

import rt.notifier.Notifier;
import rt.model.ai.DatabaseContext;
import rt.model.ai.QueryContext;
import rt.model.db_info.DatabaseStats;
import rt.model.document.InfoToShow;
import rt.model.document.DocumentRecord;
import rt.model.notification.Notification;
import rt.model.noun.Noun;

import java.sql.*;
import java.util.*;

public class DatabaseManager {

    final static String DB_URL = "jdbc:sqlite:./db/records.db";
    private QueryContext queryContext;
    private final SQLiteDatabaseInfo databaseInfo;
    private final SQLiteDataInput dataInput;
    private final SQLiteDataOutput dataOutput;
    private final SQLiteCSVExport csvExport;

    public DatabaseManager() {
        this.databaseInfo = new SQLiteDatabaseInfo();
        this.dataInput = new SQLiteDataInput();
        this.dataOutput = new SQLiteDataOutput();
        this.csvExport = new SQLiteCSVExport();
        DatabaseUtils.checkDbFolder();
        databaseInfo.createTables();
    }

    public void createRecord(DocumentRecord documentRecord) {
        try {
            dataInput.addRecord(documentRecord);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
        }
    }

    public void exportToCsv() {
        try {
            csvExport.exportToCsv();
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
        }
    }

    public Map<Long, float[]> getMessageIdsAndEmbeddings() {
        try {
            return dataOutput.getMessageIdsAndEmbeddingsAsMap(queryContext);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return Map.of();
        }
    }

    public List<InfoToShow> getMessagesByIds(Collection<Long> ids) {
        try {
            return dataOutput.getMessagesByIds(List.copyOf(ids));
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return List.of();
        }
    }

    public List<InfoToShow> searchMessagesExact(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) return List.of();

        try {
            return dataOutput.searchMessagesExact(query, queryContext, limit);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return List.of();
        }
    }

    public List<InfoToShow> searchMessagesByNouns(List<Noun> nouns, int limit) {
        if (nouns == null || nouns.isEmpty() || limit <= 0) return List.of();

        try {
            return dataOutput.searchMessagesByNouns(nouns, queryContext, limit);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return List.of();
        }
    }

    public List<InfoToShow> searchMessagesByTopic(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) return List.of();

        try {
            return dataOutput.searchMessagesByTopic(query, queryContext, limit);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return List.of();
        }
    }

    public List<InfoToShow> searchMessagesByEntities(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) return List.of();

        try {
            List<Long> entityIds = dataOutput.findEntityIds(query);
            if (entityIds.isEmpty()) return List.of();

            List<Long> messageIds = dataOutput.findMessageIdsByEntityIds(entityIds, queryContext, limit);
            if (messageIds.isEmpty()) return List.of();

            return dataOutput.getMessagesByIds(messageIds);

        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return List.of();
        }
    }

    public List<InfoToShow> getLastMessages(int limit) {
        try {
            return dataOutput.getLastMessages(queryContext, limit);
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.getMessage());
            return List.of();
        }
    }

    public DatabaseStats getDatabaseStats() {
        try {
            return databaseInfo.getDatabaseStats(queryContext);
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
            return databaseInfo.getDatabaseContext();
        } catch (SQLException e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, e.getMessage());
            return new DatabaseContext(null, null, null);
        }
    }

    public void setQueryContext(QueryContext queryContext) {
        this.queryContext = queryContext;
    }
}