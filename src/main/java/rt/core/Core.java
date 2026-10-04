package rt.core;

import it.tdlight.client.SimpleTelegramClientFactory;
import rt.ai.Agent;
import rt.api.ExternalAPIHandler;
import rt.common_utils.DateTimeUtils;
import rt.data_processing.DataProcessor;
import rt.data_processing.embedder.EmbeddingClient;
import rt.data_processing.noun_extractor.NounExtractor;
import rt.model.ai.DatabaseContext;
import rt.model.ai.QueryContext;
import rt.model.ai.Usage;
import rt.model.document.RawMessage;
import rt.model.notification.Notification;
import rt.notifier.Notifier;
import rt.storage.DatabaseManager;
import rt.telegram.TgClientWrapper;
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

public class Core implements AssistantParser, AssistantAgent {

    private TgClientWrapper tgClientWrapper;
    private final AuthUI authUI;
    private final MainView view;
    private final DatabaseManager db;
    private final ExternalAPIHandler apiHandler;
    private final DataProcessor dataProcessor;
    private final Agent agent;
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory();

    private volatile boolean isExporting;
    private volatile boolean isParsing;

    public Core() {

        this.authUI = new AuthUI();
        this.view = new MainView();
        view.setCore(this);
        this.db = new DatabaseManager();
        this.apiHandler = new ExternalAPIHandler();
        EmbeddingClient embeddingClient = new EmbeddingClient(apiHandler);
        NounExtractor nounExtractor = new NounExtractor(apiHandler);
        this.dataProcessor = new DataProcessor(db, embeddingClient, nounExtractor);
        this.agent = new Agent(apiHandler, db, embeddingClient, nounExtractor, this);
    }

    public void init() {
        executor.execute(view::start);
        executor.execute(this::start);
        executor.execute(dataProcessor::start);
    }

    @Override
    public void showQrCode(String link) {
        authUI.showQrCode(link);
    }

    @Override
    public void closeAuthWindow() {
        authUI.closeAuthWindow();
    }

    @Override
    public void addRawMessageRecord(RawMessage rawMessage) {
        dataProcessor.addRawMessageRecord(rawMessage);
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
        dataProcessor.exportToCSV();
        isExporting = false;
        Notifier.instance().add(Notification.Level.SHOW_USER, "Экспорт завершён");
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

    private boolean aiServiceIsAvailable() {
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
        db.setQueryContext(queryContext);
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

    @Override
    public void sendUsage(Usage usage) {
        view.showTokenUsage(usage);
    }

    public boolean isThinking() {
        return agent.isThinking();
    }

    public boolean isBusy() {
        return !dataProcessor.queueIsEmpty() || isExporting || isParsing;
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

    private void start() {
        try {
            tgClientWrapper = new TgClientWrapper(clientFactory, this);
            tgClientWrapper.waitForExit();
            Thread.sleep(100); // ожидание завершения соединения с TDLib
        } catch (Exception e) {
            System.err.println("Исключение в главном потоке: " + e.getMessage());
        }
    }

    public void closeApp() {
        if (tgClientWrapper != null) {
            tgClientWrapper.close();
        }
        clientFactory.close();
        dataProcessor.stop();
        executor.shutdown();
    }
}