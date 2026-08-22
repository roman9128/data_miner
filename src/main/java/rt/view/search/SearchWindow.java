package rt.view.search;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import rt.common.Core;
import rt.common.Notifier;
import rt.common.entities_and_dtos.Notification;
import rt.view.notification.NotificationUIBridge;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SearchWindow {

    private Core core;
    private Stage stage;
    private ScheduledExecutorService sourceUpdater;
    private ScheduledExecutorService queueUpdater;
    private NotificationUIBridge notificationUIBridge;
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
                Notifier.instance().add(
                        Notification.Level.ONLY_TO_LOG,
                        e.toString()
                );
            }
        });
    }

    private void show() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/rt/view/search.fxml"));
        Parent root = loader.load();
        SearchController controller = loader.getController();
        stage = new Stage();
        stage.setTitle("Telegram Data Miner");
        stage.setScene(new Scene(root, 700, 700));
        controller.setCore(core);
        controller.setStage(stage);
        notificationUIBridge = new NotificationUIBridge();
        notificationUIBridge.setController(controller);
        notificationUIBridge.start();
        updateSources(controller);
        startSourceUpdater(controller);
        startQueueUpdater(controller);
        stage.show();
        stage.setOnCloseRequest(event -> {
            event.consume();
            Platform.runLater(() -> {
                stopSourceUpdater();
                stopQueueUpdater();
                if (notificationUIBridge != null) {
                    notificationUIBridge.stop();
                    notificationUIBridge = null;
                }
                core.close();
                stage.close();
                Platform.exit();
            });
        });
    }

    private void startSourceUpdater(SearchController controller) {
        sourceUpdater = Executors.newSingleThreadScheduledExecutor();
        sourceUpdater.scheduleWithFixedDelay(() -> updateSources(controller), 5, 10, TimeUnit.SECONDS);
    }

    private void startQueueUpdater(SearchController controller) {
        queueUpdater = Executors.newSingleThreadScheduledExecutor();
        queueUpdater.scheduleWithFixedDelay(() -> Platform.runLater(
                () -> controller.updateQueueSize(core.getQueueSize())), 0, 1, TimeUnit.SECONDS
        );
    }

    private void updateSources(SearchController controller) {
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

    private void stopSourceUpdater() {
        if (sourceUpdater == null) {
            return;
        }
        sourceUpdater.shutdownNow();
        sourceUpdater = null;
    }

    private void stopQueueUpdater() {
        if (queueUpdater == null) {
            return;
        }
        queueUpdater.shutdownNow();
        queueUpdater = null;
    }
}