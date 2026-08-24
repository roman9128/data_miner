package rt.common;

import it.tdlight.client.SimpleTelegramClientFactory;
import javafx.application.Platform;
import rt.common.entities_and_dtos.Notification;
import rt.common.entities_and_dtos.RawMessageRecord;
import rt.common.utils.DateTimeUtils;
import rt.data.DataService;
import rt.telegram.TgClientWrapper;
import rt.view.auth.AuthUI;
import rt.view.search.SearchWindow;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class Core implements ParserAssistant {

    private TgClientWrapper tgClientWrapper;
    private final AuthUI authUI;
    private final SearchWindow view;
    private final DataService dataService;
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory();

    private boolean isExporting;
    private boolean isParsing;

    public Core() {
        Platform.startup(() -> {
        });
        this.authUI = new AuthUI();
        this.view = new SearchWindow();
        view.setCore(this);
        this.dataService = new DataService();
    }

    public void start() {
        executor.execute(this::startParser);
    }

    @Override
    public void showQrCode(String link) {
        authUI.showQrCode(link);
    }

    @Override
    public void startInteractions() {
        authUI.closeAuthWindow();
        executor.execute(view::startInteractions);
    }

    @Override
    public void addRawMessageRecord(RawMessageRecord rawMessageRecord) {
        dataService.addRawMessageRecord(rawMessageRecord);
    }

    public void close() {
        tgClientWrapper.close();
    }

    public void parseMessages(Set<Long> source, LocalDate dateFrom, LocalDate dateTo) {
        if (!dataService.queueIsEmpty() || isExporting || isParsing) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Сейчас немного занят, подождите");
            return;
        }

        if (source.isEmpty()) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Не выбраны источники");
            return;
        }

        isParsing = true;
        Set<Long> senderIds = prepareSenderIds(source, tgClientWrapper::getChatsInFolder);
        long unixDateFrom = DateTimeUtils.getUnixDateFrom(dateFrom);
        long unixDateTo = DateTimeUtils.getUnixDateTo(dateTo);
        executor.execute(dataService::work);

        executor.submit(() -> {
            senderIds.forEach(channelId -> tgClientWrapper.loadChannelsHistory(channelId, unixDateFrom, unixDateTo));
            Notifier.instance().add(Notification.Level.SHOW_USER, "Загрузка завершена");
            isParsing = false;
        });
    }

    public void exportToCSV() {
        if (!dataService.queueIsEmpty() || isExporting || isParsing) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Сейчас немного занят, подождите");
            return;
        }
        Notifier.instance().add(Notification.Level.SHOW_USER, "Начинаю экспорт базы данных");
        isExporting = true;
        dataService.exportToCSV();
        isExporting = false;
        Notifier.instance().add(Notification.Level.SHOW_USER, "Экспорт завершён");
    }

    public Map<Integer, String> getFoldersIDsAndNames() {
        return tgClientWrapper.getFoldersIDsAndNames();
    }

    public Map<Long, String> getChannelsIDsAndNames() {
        return tgClientWrapper.getChannelsIDsAndNames();
    }

    public int getQueueSize() {
        int queueSize = dataService.getQueueSize();
        if (queueSize != 0) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Сообщений в обработке: " + queueSize);
        }
        return queueSize;
    }

    private Set<Long> prepareSenderIds(Set<Long> source, Function<Integer, Collection<Long>> getFolder) {
        Set<Long> result = new TreeSet<>();
        source.forEach(n -> {
            if (n < 0) {
                result.add(n);
            } else if (n > 0) {
                result.addAll(getFolder.apply(n.intValue()));
            }
        });
        return result;
    }

    private void startParser() {
        try {
            tgClientWrapper = new TgClientWrapper(clientFactory, this);
            Notifier.instance().add(Notification.Level.SHOW_USER, "Готов к работе");
            tgClientWrapper.waitForExit();
            Thread.sleep(100); // ожидание завершения соединения с TDLib
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Исключение в главном потоке: " + e.getMessage());
        } finally {
            closeApp();
        }
    }

    private void closeApp() {
        clientFactory.close();
        dataService.stop();
        executor.shutdown();
    }
}