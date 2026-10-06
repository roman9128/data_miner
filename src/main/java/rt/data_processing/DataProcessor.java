package rt.data_processing;

import rt.common_utils.TextStatisticsCalculator;
import rt.data_processing.classifier.Classifier;
import rt.data_processing.embedder.EmbeddingClient;
import rt.data_processing.ner.NERService;
import rt.data_processing.noun_extractor.NounExtractor;
import rt.model.document.DocumentRecord;
import rt.model.document.RawMessage;
import rt.model.document.TextStatistics;
import rt.model.notification.Notification;
import rt.notifier.Notifier;
import rt.storage.DatabaseManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DataProcessor {

    private final DatabaseManager db;
    private final NERService nerService;
    private final Classifier classifier;
    private final NounExtractor nounExtractor;
    private final EmbeddingClient embeddingClient;
    private final LinkedBlockingQueue<RawMessage> rawMessages = new LinkedBlockingQueue<>(1000);
    private volatile boolean running = false;

    public DataProcessor(DatabaseManager db, EmbeddingClient embeddingClient, NounExtractor nounExtractor) {
        this.db = db;
        this.nerService = new NERService();
        this.nounExtractor = nounExtractor;
        this.embeddingClient = embeddingClient;
        this.classifier = new Classifier(embeddingClient);
    }

    public void exportToCSV() {
        db.exportToCsv();
    }

    public boolean queueIsEmpty() {
        return rawMessages.isEmpty();
    }

    public void addRawMessageRecord(RawMessage rawMessage) {
        try {
            rawMessages.put(rawMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, e.toString());
        }
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        try {
            while (running) {
                var rm = rawMessages.poll(1, TimeUnit.SECONDS);
                if (rm != null) {
                    processRawMessage(rm);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running = false;
        }
    }

    public void stop() {
        running = false;
    }

    public int getQueueSize() {
        return rawMessages.size();
    }

    private void processRawMessage(RawMessage rawMessage) {
        String text = rawMessage.text();
        if (text == null || text.isBlank()) return;
        TextStatistics textStatistics = TextStatisticsCalculator.calculate(text);
        LocalDateTime messageDateTime = rawMessage.dateTime();
        float[] textEmb = embeddingClient.createEmbedding(text);

        DocumentRecord documentRecord = new DocumentRecord(
                rawMessage.sourceDocumentId(),
                rawMessage.sourceId(),
                rawMessage.sourceName(),
                rawMessage.link(),
                LocalDateTime.now(ZoneId.systemDefault()),
                messageDateTime,
                messageDateTime.getYear(),
                messageDateTime.getMonth(),
                messageDateTime.getDayOfMonth(),
                messageDateTime.getDayOfWeek(),
                messageDateTime.getHour(),
                messageDateTime.getMinute(),
                messageDateTime.getSecond(),
                rawMessage.contentType().name(),
                rawMessage.contentSource().name(),
                text,
                text.length(),
                textStatistics.wordCount(),
                textStatistics.emojiCount(),
                nounExtractor.extract(text),
                nerService.extractEntities(text),
                classifier.classify(text, textEmb),
                textEmb
        );
        db.createRecord(documentRecord);
    }
}
