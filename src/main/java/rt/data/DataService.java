package rt.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import rt.common.Notifier;
import rt.common.entities_and_dtos.*;
import rt.common.entities_and_dtos.MessageContentType;
import rt.common.utils.DateTimeUtils;
import rt.data.analyzer.Analyzer;
import rt.data.noun_extractor.NounExtractor;
import rt.data.stats.TextStatisticsCalculator;
import rt.data.storage.SQLiteDB;

import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DataService {

    private final SQLiteDB db;
    private final Analyzer analyzer;
    private final NounExtractor nounExtractor;
    private final LinkedBlockingQueue<RawMessageRecord> rawMessageRecords = new LinkedBlockingQueue<>(1000);
    private volatile boolean running = false;

    public DataService() {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        this.db = new SQLiteDB();
        this.analyzer = new Analyzer();
        this.nounExtractor = new NounExtractor(client, objectMapper);
    }

    public void exportToCSV() {
        db.exportToCsv();
    }

    public boolean queueIsEmpty() {
        return rawMessageRecords.isEmpty();
    }

    public void addRawMessageRecord(RawMessageRecord rawMessageRecord) {
        try {
            rawMessageRecords.put(rawMessageRecord);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Notifier.instance().add(Notification.Level.SHOW_USER, e.toString());
        }
    }

    public void work() {
        if (running) {
            return;
        }
        running = true;
        try {
            while (running) {
                var rmr = rawMessageRecords.poll(300, TimeUnit.MILLISECONDS);
                if (rmr != null) {
                    processRawMessageRecord(rmr);
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
        return rawMessageRecords.size();
    }

    private void processRawMessageRecord(RawMessageRecord rawMessageRecord) {
        LocalDateTime messageDateTime = DateTimeUtils.getDateTime(rawMessageRecord.message().date);
        MessageTypeText messageTypeText = extractTypeAndTextFromMessage(rawMessageRecord.message());
        String text = messageTypeText.text();
        TextStatistics textStatistics = TextStatisticsCalculator.calculate(text);

        MessageRecord messageRecord = new MessageRecord(
                rawMessageRecord.message().id,
                rawMessageRecord.message().chatId,
                rawMessageRecord.chatName(),
                rawMessageRecord.link(),
                LocalDateTime.now(ZoneId.systemDefault()),
                messageDateTime.getYear(),
                messageDateTime.getMonth(),
                messageDateTime.getDayOfMonth(),
                messageDateTime.getDayOfWeek(),
                messageDateTime.getHour(),
                messageDateTime.getMinute(),
                messageDateTime.getSecond(),
                messageTypeText.type(),
                messageTypeText.text(),
                messageTypeText.text().length(),
                textStatistics.wordCount(),
                textStatistics.averageWordLength(),
                textStatistics.emojiCount(),
                nounExtractor.extract(text),
                analyzer.recognizeNE(text),
                analyzer.classify(text)
        );
        db.createRecord(messageRecord);
    }

    private MessageTypeText extractTypeAndTextFromMessage(TdApi.Message message) {
        TdApi.MessageContent messageContent = message.content;
        switch (messageContent) {
            case TdApi.MessageText text -> {
                return new MessageTypeText(
                        MessageContentType.TEXT,
                        getRidOfNull(text.text.text));
            }

            case TdApi.MessagePhoto photo -> {
                return new MessageTypeText(
                        MessageContentType.PHOTO,
                        getRidOfNull(photo.caption.text));
            }
            case TdApi.MessageVideo video -> {
                return new MessageTypeText(
                        MessageContentType.VIDEO,
                        getRidOfNull(video.caption.text));
            }
            case TdApi.MessageDocument document -> {
                return new MessageTypeText(
                        MessageContentType.DOCUMENT,
                        getRidOfNull(document.caption.text));
            }
            case TdApi.MessageAudio audio -> {
                return new MessageTypeText(
                        MessageContentType.AUDIO,
                        getRidOfNull(audio.caption.text));
            }
            case TdApi.MessagePoll poll -> {
                return new MessageTypeText(
                        MessageContentType.POLL,
                        getRidOfNull(poll.poll.question));
            }
            case TdApi.MessageAnimation animation -> {
                return new MessageTypeText(
                        MessageContentType.ANIMATION,
                        getRidOfNull(animation.caption.text));
            }
            default -> {
                return new MessageTypeText(
                        MessageContentType.MISC,
                        getRidOfNull(messageContent.toString())
                );
            }
        }
    }

    private String getRidOfNull(String text) {
        return text == null ? "" : text;
    }
}
