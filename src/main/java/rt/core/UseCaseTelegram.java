package rt.core;

import it.tdlight.client.SimpleTelegramClientFactory;
import rt.api.ExternalAPI;
import rt.common_utils.DateTime;
import rt.common_utils.Numbers;
import rt.data_processing.DataProcessor;
import rt.model.document.RawMessage;
import rt.model.notification.Notification;
import rt.notifier.Notifier;
import rt.telegram.TgClientWrapper;
import rt.view.auth.AuthUI;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class UseCaseTelegram implements AssistantParser {

    private TgClientWrapper tgClientWrapper;
    private final Core core;
    private final ExecutorService executor;
    private final AuthUI authUI;
    private final DataProcessor dataProcessor;
    private final SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory();

    private volatile boolean isParsing;

    public UseCaseTelegram(Core core, ExecutorService executor, DataProcessor dataProcessor) {
        this.core = core;
        this.executor = executor;
        this.dataProcessor = dataProcessor;
        this.authUI = new AuthUI();
    }

    @Override
    public void showTelegramQrCode(String link) {
        authUI.showQrCode(link);
    }

    @Override
    public void closeTelegramAuthWindow() {
        authUI.closeAuthWindow();
    }

    @Override
    public void addRawMessage(RawMessage rawMessage) {
        dataProcessor.addRawMessageRecord(rawMessage);
    }

    public void parseTelegramMessages(Set<Long> source, LocalDate dateFrom, LocalDate dateTo) {
        if (!ExternalAPI.checkHealth()) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Вспомогательный сервис для анализа текста недоступен. Проверьте, что он запущен в Docker");
        }

        if (core.isBusy()) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Сейчас немного занят, подождите");
            return;
        }

        if (source.isEmpty()) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Не выбраны источники");
            return;
        }

        isParsing = true;
        Set<Long> senderIds = Numbers.prepareSenderIds(source, tgClientWrapper::getChatsInFolder);
        long unixDateFrom = DateTime.getUnixDateFrom(dateFrom);
        long unixDateTo = DateTime.getUnixDateTo(dateTo);

        executor.submit(() -> {
            senderIds.forEach(channelId -> tgClientWrapper.loadChannelsHistory(channelId, unixDateFrom, unixDateTo));
            Notifier.instance().add(Notification.Level.SHOW_USER, "Загрузка завершена");
            isParsing = false;
        });
    }

    public Map<Integer, String> getFoldersIDsAndNames() {
        if (tgClientWrapper == null) return Map.of();
        return tgClientWrapper.getFoldersIDsAndNames();
    }

    public Map<Long, String> getChannelsIDsAndNames() {
        if (tgClientWrapper == null) return Map.of();
        return tgClientWrapper.getChannelsIDsAndNames();
    }

    public int getQueueSize() {
        int queueSize = dataProcessor.getQueueSize();
        if (queueSize != 0) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Сообщений в обработке: " + queueSize);
        }
        return queueSize;
    }

    boolean isParsing() {
        return isParsing;
    }

    void start() {
        try {
            tgClientWrapper = new TgClientWrapper(clientFactory, this);
            tgClientWrapper.waitForExit();
            Thread.sleep(100); // ожидание завершения соединения с TDLib
        } catch (Exception e) {
            System.err.println("Исключение в главном потоке: " + e.getMessage());
        }
    }

    void stop() {
        if (tgClientWrapper != null) {
            tgClientWrapper.close();
        }
        clientFactory.close();
    }
}