package rt.view.main;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import rt.core.Core;
import rt.core.Notifier;
import rt.model.notification.Notification;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainView {
    private Core core;
    private Stage stage;
    private Controller controller;
    private ScheduledExecutorService sourceUpdater;
    private ScheduledExecutorService queueUpdater;
    private NotificationHandler notificationHandler;
    private Map<Integer, String> lastFolders = Map.of();
    private Map<Long, String> lastChannels = Map.of();

    public void setCore(Core core) {
        this.core = core;
    }

    public void startInteractions() {
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
        controller.setCore(core);
        controller.loadDatabaseContext();
        notificationHandler = new NotificationHandler();
        notificationHandler.setController(controller);
        notificationHandler.start();
        updateSources(controller);
        startSourceUpdater(controller);
        startQueueUpdater(controller);
        stage.show();
        stage.setOnCloseRequest(event -> {
            event.consume();
            Platform.runLater(() -> {
                stopSourceUpdater();
                stopQueueUpdater();
                if (notificationHandler != null) {
                    notificationHandler.stop();
                    notificationHandler = null;
                }
                core.close();
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
        Map<Integer, String> folders = core.getFoldersIDsAndNames();
        Map<Long, String> channels = core.getChannelsIDsAndNames();
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
        if (core.isThinking() || core.isBusy()) return;
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
                () -> controller.updateQueueSize(core.getQueueSize())), 0, 1, TimeUnit.SECONDS
        );
    }

    private void stopQueueUpdater() {
        if (queueUpdater == null) {
            return;
        }
        queueUpdater.shutdownNow();
        queueUpdater = null;
    }
}