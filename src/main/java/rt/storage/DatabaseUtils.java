package rt.storage;

import rt.model.ai.QueryContext;
import rt.model.ne.NamedEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class DatabaseUtils {

    private DatabaseUtils() {
    }

    static void checkDbFolder() {
        final Path dbFolder = Path.of("db");

        if (!Files.exists(dbFolder)) {
            try {
                Files.createDirectories(dbFolder);
            } catch (IOException e) {
                System.err.println("Ошибка при создании папки для базы данных: " + e);
            }
        }
    }

    static Filter buildMessageFilter(QueryContext context) {
        if (context == null) {
            return new Filter("", List.of());
        }
        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        if (context.chatIds() != null && !context.chatIds().isEmpty()) {

            String placeholders = context.chatIds().stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(", "));
            conditions.add("m.source_id IN (" + placeholders + ")");
            parameters.addAll(context.chatIds());
        }

        if (context.dateFrom() != null) {
            LocalDate dateFrom = context.dateFrom();
            conditions.add("""
                    (
                        m.publish_year > ?
                        OR (
                            m.publish_year = ?
                            AND m.publish_month > ?
                        )
                        OR (
                            m.publish_year = ?
                            AND m.publish_month = ?
                            AND m.publish_day >= ?
                        )
                    )
                    """);

            parameters.add(dateFrom.getYear());
            parameters.add(dateFrom.getYear());
            parameters.add(dateFrom.getMonthValue());
            parameters.add(dateFrom.getYear());
            parameters.add(dateFrom.getMonthValue());
            parameters.add(dateFrom.getDayOfMonth());
        }

        if (context.dateTo() != null) {
            LocalDate dateTo = context.dateTo();
            conditions.add("""
                    (
                        m.publish_year < ?
                        OR (
                            m.publish_year = ?
                            AND m.publish_month < ?
                        )
                        OR (
                            m.publish_year = ?
                            AND m.publish_month = ?
                            AND m.publish_day <= ?
                        )
                    )
                    """);

            parameters.add(dateTo.getYear());
            parameters.add(dateTo.getYear());
            parameters.add(dateTo.getMonthValue());
            parameters.add(dateTo.getYear());
            parameters.add(dateTo.getMonthValue());
            parameters.add(dateTo.getDayOfMonth());
        }

        if (context.namedEntities() != null && !context.namedEntities().isEmpty()) {

            String placeholders = context.namedEntities().stream()
                    .map(entity -> "?")
                    .collect(Collectors.joining(", "));

            conditions.add("""
                    EXISTS (
                        SELECT 1
                        FROM message_entities me_filter
                        JOIN named_entities ne_filter
                            ON ne_filter.entity_id = me_filter.entity_id
                        WHERE me_filter.message_id = m.id
                          AND ne_filter.entity_name IN (%s)
                    )
                    """.formatted(placeholders));

            parameters.addAll(
                    context.namedEntities().stream()
                            .map(NamedEntity::getName)
                            .toList()
            );
        }

        if (context.topics() != null && !context.topics().isEmpty()) {

            List<String> topicConditions = new ArrayList<>();

            for (String entry : context.topics()) {

                topicConditions.add("""
                        EXISTS (
                            SELECT 1
                            FROM message_topics mt_filter
                            JOIN topics t_filter
                                ON t_filter.topic_id = mt_filter.topic_id
                            WHERE mt_filter.message_id = m.id
                              AND t_filter.topic_name = ?
                        )
                        """);
                parameters.add(entry);
            }
            conditions.add("(" + String.join(" OR ", topicConditions) + ")");
        }

        if (conditions.isEmpty()) {
            return new Filter("", List.of());
        }

        return new Filter(" WHERE " + String.join(" AND ", conditions), parameters);
    }

    static void setParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }
}
