package rt.view.main;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import rt.core.UseCaseAi;
import rt.core.Core;
import rt.core.UseCaseCsvExport;
import rt.core.UseCaseTelegram;
import rt.model.ai.DatabaseContext;
import rt.model.ai.QueryContext;
import rt.notifier.Notifier;
import rt.model.ai.Usage;
import rt.model.notification.Notification;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainView {

    private Core core;
    private UseCaseAi useCaseAi;
    private UseCaseCsvExport useCaseCsvExport;
    private UseCaseTelegram useCaseTelegram;
    private Stage stage;
    private Controller controller;
    private ScheduledExecutorService sourceUpdater;
    private ScheduledExecutorService queueUpdater;
    private ScheduledExecutorService notificationUpdater;
    private Map<Integer, String> lastFolders = Map.of();
    private Map<Long, String> lastChannels = Map.of();

    public void setCore(Core core) {
        this.core = core;
    }

    public void setUseCases(UseCaseAi useCaseAi, UseCaseCsvExport useCaseCsvExport, UseCaseTelegram useCaseTelegram) {
        this.useCaseAi = useCaseAi;
        this.useCaseCsvExport = useCaseCsvExport;
        this.useCaseTelegram = useCaseTelegram;
    }

    public void start() {
        Platform.startup(() -> {
        });

        Platform.runLater(() -> {
            try {
                show();
            } catch (IOException e) {
                Notifier.instance().add(Notification.Level.ONLY_TO_LOG, e.toString());
            }
        });
    }

    public void showAgentsAnswer(String answer) {
        controller.showAgentsAnswer(answer);
    }

    public void showTokenUsage(Usage usage) {
        controller.showTokenUsage(usage);
    }

    private void show() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/rt/view/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage = new Stage();
        stage.setTitle("Telegram Data Miner");
        Scene scene = new Scene(root, 700, 700);
        scene.getStylesheets().add(getClass().getResource("/rt/view/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(700);
        stage.setMinHeight(700);
        controller.setMainView(this);
        controller.loadDatabaseContext();
        updateSources(controller);
        startSourceUpdater(controller);
        startQueueUpdater(controller);
        startNotificationUpdater(controller);
        stage.show();
        stage.setOnCloseRequest(event -> {
            event.consume();
            Platform.runLater(() -> {
                stopSourceUpdater();
                stopQueueUpdater();
                stopNotificationUpdater();
                core.closeApp();
                stage.close();
                Platform.exit();
            });
        });
    }

    private void startSourceUpdater(Controller controller) {
        sourceUpdater = Executors.newSingleThreadScheduledExecutor();
        sourceUpdater.scheduleWithFixedDelay(() -> updateSources(controller), 5, 10, TimeUnit.SECONDS);
    }

    private void updateSources(Controller controller) {
        updateContextSources();
        Map<Integer, String> folders = useCaseTelegram.getFoldersIDsAndNames();
        Map<Long, String> channels = useCaseTelegram.getChannelsIDsAndNames();
        boolean foldersChanged = !folders.equals(lastFolders);
        boolean channelsChanged = !channels.equals(lastChannels);

        if (!foldersChanged && !channelsChanged) {
            return;
        }
        lastFolders = Map.copyOf(folders);
        lastChannels = Map.copyOf(channels);
        Platform.runLater(() -> {
            if (foldersChanged) {
                controller.setFolders(folders);
            }
            if (channelsChanged) {
                controller.setChannels(channels);
            }
        });
    }

    private void updateContextSources() {
        if (useCaseAi.isThinking() || core.isBusy()) return;
        Platform.runLater(() -> controller.loadDatabaseContext());
    }

    private void stopSourceUpdater() {
        if (sourceUpdater == null) {
            return;
        }
        sourceUpdater.shutdownNow();
        sourceUpdater = null;
    }

    private void startQueueUpdater(Controller controller) {
        queueUpdater = Executors.newSingleThreadScheduledExecutor();
        queueUpdater.scheduleWithFixedDelay(() -> Platform.runLater(
                () -> controller.updateQueueSize(useCaseTelegram.getQueueSize())), 0, 1, TimeUnit.SECONDS
        );
    }

    private void stopQueueUpdater() {
        if (queueUpdater == null) {
            return;
        }
        queueUpdater.shutdownNow();
        queueUpdater = null;
    }

    private void startNotificationUpdater(Controller controller) {
        notificationUpdater = Executors.newSingleThreadScheduledExecutor();
        notificationUpdater.scheduleWithFixedDelay(
                () -> updateNotification(controller),
                2000,
                500,
                TimeUnit.MILLISECONDS
        );
    }

    private void updateNotification(Controller controller) {
        try {
            Notification n = Notifier.instance().poll();
            if (n != null) {
                String text = n.text();
                if (text == null || text.isBlank()) return;
                Platform.runLater(() -> controller.addNotification(text));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void stopNotificationUpdater() {
        if (notificationUpdater == null) {
            return;
        }
        notificationUpdater.shutdownNow();
        notificationUpdater = null;
        Notifier.shutdownLogger();
    }

    DatabaseContext getDatabaseContext() {
        return useCaseAi.getDatabaseContext();
    }

    void setQueryContext(QueryContext queryContext) {
        useCaseAi.setQueryContext(queryContext);
    }

    void exportToCSV() {
        useCaseCsvExport.exportToCsv();
    }

    void parseTelegramMessages(Set<Long> source, LocalDate from, LocalDate to) {
        useCaseTelegram.parseTelegramMessages(source, from, to);
    }

    boolean isThinking() {
        return useCaseAi.isThinking();
    }

    boolean isBusy() {
        return core.isBusy();
    }

    void askAgent(String message) {
        useCaseAi.askAgent(message);
    }

    void clearChat() {
        useCaseAi.clearChat();
    }
}