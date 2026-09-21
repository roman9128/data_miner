package rt.core;

import it.tdlight.client.SimpleTelegramClientFactory;
import javafx.application.Platform;
import rt.ai.Agent;
import rt.api.ExternalAPIHandler;
import rt.data_processing.DataInputService;
import rt.data_processing.embedder.EmbeddingClient;
import rt.model.ai.DatabaseContext;
import rt.storage.SQLiteDB;
import rt.model.ai.QueryContext;
import rt.model.message.RawMessageRecord;
import rt.model.notification.Notification;
import rt.telegram.TgClientWrapper;
import rt.utils.DateTimeUtils;
import rt.view.auth.AuthUI;
import rt.view.main.MainView;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class Core implements ParserAssistant, AgentAssistant {

    private TgClientWrapper tgClientWrapper;
    private final AuthUI authUI;
    private final MainView view;
    private final SQLiteDB db;
    private final ExternalAPIHandler apiHandler;
    private final DataInputService dataInputService;
    private final Agent agent;
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory();

    private volatile boolean isExporting;
    private volatile boolean isParsing;

    public Core() {
        Platform.startup(() -> {
        });
        this.authUI = new AuthUI();
        this.view = new MainView();
        view.setCore(this);
        this.db = new SQLiteDB();
        this.apiHandler = new ExternalAPIHandler();
        EmbeddingClient embeddingClient = new EmbeddingClient(apiHandler);
        this.dataInputService = new DataInputService(apiHandler, db, embeddingClient);
        this.agent = new Agent(apiHandler, db, embeddingClient, this);
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
        dataInputService.addRawMessageRecord(rawMessageRecord);
    }

    public void close() {
        tgClientWrapper.close();
    }

    public void parseMessages(Set<Long> source, LocalDate dateFrom, LocalDate dateTo) {
        if (!aiServiceIsAvailable()) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Вспомогательный сервис для анализа текста недоступен. Проверьте, что он запущен в Docker");
        }

        if (isBusy()) {
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
        executor.execute(dataInputService::work);

        executor.submit(() -> {
            senderIds.forEach(channelId -> tgClientWrapper.loadChannelsHistory(channelId, unixDateFrom, unixDateTo));
            Notifier.instance().add(Notification.Level.SHOW_USER, "Загрузка завершена");
            isParsing = false;
        });
    }

    public void exportToCSV() {
        if (isBusy()) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Сейчас немного занят, подождите");
            return;
        }
        Notifier.instance().add(Notification.Level.SHOW_USER, "Начинаю экспорт базы данных");
        isExporting = true;
        dataInputService.exportToCSV();
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
        int queueSize = dataInputService.getQueueSize();
        if (queueSize != 0) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Сообщений в обработке: " + queueSize);
        }
        return queueSize;
    }

    public boolean aiServiceIsAvailable() {
        return apiHandler.checkHealth();
    }

    public DatabaseContext getDatabaseContext() {
        if (isBusy()) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Сейчас немного занят, подождите");
            return null;
        }
        return db.getDatabaseContext();
    }

    public void setQueryContext(QueryContext queryContext) {
        agent.setQueryContext(queryContext);
    }

    public void clearChat() {
        agent.clearChat();
    }

    public void askAgent(String question) {
        if (isBusy()) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Сейчас немного занят, подождите");
            return;
        }
        executor.execute(() -> agent.ask(question));
    }

    @Override
    public void sendAnswer(String answer) {
        view.showAgentsAnswer(answer);
    }

    public boolean isThinking() {
        return agent.isThinking();
    }

    public boolean isBusy() {
        return !dataInputService.queueIsEmpty() || isExporting || isParsing;
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
            tgClientWrapper.waitForExit();
            Thread.sleep(100); // ожидание завершения соединения с TDLib
        } catch (Exception e) {
            System.err.println("Исключение в главном потоке: " + e.getMessage());
        } finally {
            closeApp();
        }
    }

    private void closeApp() {
        clientFactory.close();
        dataInputService.stop();
        executor.shutdown();
    }
}