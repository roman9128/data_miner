package rt.data_processing;

import it.tdlight.jni.TdApi;
import rt.model.document.ContentSource;
import rt.notifier.Notifier;
import rt.data_processing.classifier.Classifier;
import rt.data_processing.embedder.EmbeddingClient;
import rt.data_processing.ner.NERService;
import rt.data_processing.noun_extractor.NounExtractor;
import rt.data_processing.stats.TextStatisticsCalculator;
import rt.model.document.DocumentRecord;
import rt.model.document.RawMessage;
import rt.model.document.TextStatistics;
import rt.model.notification.Notification;
import rt.storage.DatabaseManager;
import rt.common_utils.DateTimeUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
                    processRawMessageRecord(rm);
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

    private void processRawMessageRecord(RawMessage rawMessage) {
        MessageTypeText messageTypeText = extractTypeAndTextFromMessage(rawMessage.message());
        String text = messageTypeText.text();
        if (text == null || text.isBlank()) return;
        TextStatistics textStatistics = TextStatisticsCalculator.calculate(text);
        LocalDateTime messageDateTime = DateTimeUtils.getDateTime(rawMessage.message().date);
        float[] textEmb = embeddingClient.createEmbedding(text);

        DocumentRecord documentRecord = new DocumentRecord(
                String.valueOf(rawMessage.message().id),
                String.valueOf(rawMessage.message().chatId),
                rawMessage.chatName(),
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
                messageTypeText.type().name(),
                rawMessage.source().name(),
                messageTypeText.text(),
                messageTypeText.text().length(),
                textStatistics.wordCount(),
                textStatistics.averageWordLength(),
                textStatistics.emojiCount(),
                nounExtractor.extract(text),
                nerService.extractEntities(text),
                classifier.classify(text, textEmb),
                textEmb
        );
        db.createRecord(documentRecord);
    }

    private MessageTypeText extractTypeAndTextFromMessage(TdApi.Message message) {
        TdApi.MessageContent messageContent = message.content;
        switch (messageContent) {
            case TdApi.MessageText text -> {
                return new MessageTypeText(
                        ContentType.TEXT,
                        getRidOfNull(text.text.text));
            }

            case TdApi.MessagePhoto photo -> {
                return new MessageTypeText(
                        ContentType.PHOTO,
                        getRidOfNull(photo.caption.text));
            }
            case TdApi.MessageVideo video -> {
                return new MessageTypeText(
                        ContentType.VIDEO,
                        getRidOfNull(video.caption.text));
            }
            case TdApi.MessageDocument document -> {
                return new MessageTypeText(
                        ContentType.DOCUMENT,
                        getRidOfNull(document.caption.text));
            }
            case TdApi.MessageAudio audio -> {
                return new MessageTypeText(
                        ContentType.AUDIO,
                        getRidOfNull(audio.caption.text));
            }
            case TdApi.MessagePoll poll -> {
                return new MessageTypeText(
                        ContentType.POLL,
                        getPollTexts(poll));
            }
            case TdApi.MessageAnimation animation -> {
                return new MessageTypeText(
                        ContentType.ANIMATION,
                        getRidOfNull(animation.caption.text));
            }
            case TdApi.MessageVideoNote videoNote -> {
                return new MessageTypeText(
                        ContentType.VIDEO_NOTE,
                        "");
            }
            case TdApi.MessageVoiceNote voiceNote -> {
                return new MessageTypeText(
                        ContentType.VOICE_NOTE,
                        getRidOfNull(voiceNote.caption.text));
            }
            default -> {
                return new MessageTypeText(
                        ContentType.MISC,
                        getRidOfNull(messageContent.toString())
                );
            }
        }
    }

    private String getRidOfNull(String text) {
        return text == null ? "" : text;
    }

    private String getPollTexts(TdApi.MessagePoll poll) {
        String question = getRidOfNull(poll.poll.question);
        String options = Arrays.stream(poll.poll.options)
                .map(o -> getRidOfNull(o.text))
                .filter(o -> !o.isBlank())
                .map(o -> "\n- " + o)
                .collect(Collectors.joining());
        return question + options;
    }

    private record MessageTypeText(ContentType type, String text) {
    }

    enum ContentType {
        TEXT,
        PHOTO,
        VIDEO,
        VIDEO_NOTE,
        VOICE_NOTE,
        DOCUMENT,
        AUDIO,
        POLL,
        ANIMATION,
        MISC
    }
}
