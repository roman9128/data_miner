package rt.telegram;

import it.tdlight.jni.TdApi;
import rt.common.Notifier;
import rt.common.entities_and_dtos.Notification;

import java.util.concurrent.ConcurrentLinkedDeque;

class ChatHistoryLoader {

    private final ConcurrentLinkedDeque<TdApi.Message> receivedMessages = new ConcurrentLinkedDeque<>();
    private long dateFromUnix = 0L;
    private long dateToUnix = Long.MAX_VALUE;
    private int totalCount;

    public void acceptMessages(TdApi.Messages messages) {
        for (TdApi.Message message : messages.messages) {
            receivedMessages.offer(message);
        }
        totalCount += messages.messages.length;
        Notifier.instance().add(Notification.Level.SHOW_USER, "Загружено сообщений: " + totalCount);
    }

    TdApi.Message takeMessage() {
        return receivedMessages.pollFirst();
    }

    boolean isEmpty() {
        return receivedMessages.isEmpty();
    }

    void zeroCount() {
        totalCount = 0;
    }

    void setDateFromUnix(long dateFromUnix) {
        this.dateFromUnix = dateFromUnix;
    }

    void setDateToUnix(long dateToUnix) {
        this.dateToUnix = dateToUnix;
    }

    void removeSurplus() {
        receivedMessages.removeIf(message -> message.date < dateFromUnix || message.date > dateToUnix);
    }
}