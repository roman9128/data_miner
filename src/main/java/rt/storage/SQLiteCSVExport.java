package rt.storage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

final class SQLiteCSVExport {

    private final Path exportDirectory = Path.of("dataset");

    void exportToCsv() throws SQLException, IOException {
        Files.createDirectories(exportDirectory);

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
        try (Connection connection = DriverManager.getConnection(DatabaseManager.DB_URL)) {
            for (String table : tables) {
                exportTableToCsv(connection, table, exportDirectory);
            }
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
                String columnName = metadata.getColumnName(i);
                String columnType = metadata.getColumnTypeName(i);
                if ("BLOB".equalsIgnoreCase(columnType)) {
                    writer.write(csvEscape(columnName + "_BLOB"));
                } else {
                    writer.write(csvEscape(columnName));
                }
            }
            writer.newLine();
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) {
                        writer.write(";");
                    }
                    String columnType = metadata.getColumnTypeName(i);
                    if ("BLOB".equalsIgnoreCase(columnType)) {
                        writer.write("[BLOB]");
                    } else {
                        Object value = resultSet.getObject(i);
                        if (value != null) {
                            writer.write(csvEscape(value.toString()));
                        }
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