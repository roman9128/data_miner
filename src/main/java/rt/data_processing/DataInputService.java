package rt.data_processing;

import it.tdlight.jni.TdApi;
import rt.api.ExternalAPIHandler;
import rt.core.Notifier;
import rt.model.message.*;
import rt.model.notification.Notification;
import rt.utils.DateTimeUtils;
import rt.data_processing.embedder.EmbeddingClient;
import rt.data_processing.ner.NERService;
import rt.data_processing.nlp.NLPService;
import rt.data_processing.noun_extractor.NounExtractor;
import rt.data_processing.stats.TextStatisticsCalculator;
import rt.storage.SQLiteDB;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DataInputService {

    private final SQLiteDB db;
    private final NERService nerService;
    private final NLPService nlpService;
    private final NounExtractor nounExtractor;
    private final EmbeddingClient embeddingClient;
    private final LinkedBlockingQueue<RawMessageRecord> rawMessageRecords = new LinkedBlockingQueue<>(1000);
    private volatile boolean running = false;

    public DataInputService(ExternalAPIHandler apiHandler, SQLiteDB db, EmbeddingClient embeddingClient) {

        this.db = db;
        nlpService = new NLPService();
        nerService = new NERService();
        nounExtractor = new NounExtractor(apiHandler);
        this.embeddingClient = embeddingClient;
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
        MessageTypeText messageTypeText = extractTypeAndTextFromMessage(rawMessageRecord.message());
        String text = messageTypeText.text();
        if (text == null || text.isBlank()) return;
        MessageTies messageTies = getMessageTies(rawMessageRecord.message());
        TextStatistics textStatistics = TextStatisticsCalculator.calculate(text);
        LocalDateTime messageDateTime = DateTimeUtils.getDateTime(rawMessageRecord.message().date);

        MessageRecord messageRecord = new MessageRecord(
                rawMessageRecord.message().id,
                rawMessageRecord.message().chatId,
                rawMessageRecord.chatName(),
                rawMessageRecord.link(),
                LocalDateTime.now(ZoneId.systemDefault()),
                messageDateTime,
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
                messageTies.replyToChatId(),
                messageTies.replyToMessageId(),
                messageTies.forwardOriginChatId(),
                messageTies.forwardOriginMessageId(),
                nounExtractor.extract(text),
                nerService.extractEntities(text),
                nlpService.classify(text),
                embeddingClient.createEmbedding(text)
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
                        getPollTexts(poll));
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

    private MessageTies getMessageTies(TdApi.Message message) {
        long replyToChatId = 0, replyToMessageId = 0, forwardOriginChatId = 0, forwardOriginMessageId = 0;
        if (message.replyTo != null && message.replyTo instanceof TdApi.MessageReplyToMessage messageReplyToMessage) {
            replyToChatId = messageReplyToMessage.chatId;
            replyToMessageId = messageReplyToMessage.messageId;
        }
        if (message.forwardInfo != null && message.forwardInfo.origin instanceof TdApi.MessageOriginChannel originChannel) {
            forwardOriginChatId = originChannel.chatId;
            forwardOriginMessageId = originChannel.messageId;
        }
        return new MessageTies(replyToChatId, replyToMessageId, forwardOriginChatId, forwardOriginMessageId);
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
}
