package rt.view.notification;

import javafx.application.Platform;
import rt.common.Notifier;
import rt.view.search.SearchController;

public class NotificationUIBridge {

    private volatile boolean running = false;
    private Thread notificationThread;
    private SearchController controller;

    public NotificationUIBridge() {
    }

    public void setController(SearchController controller) {
        this.controller = controller;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        notificationThread = new Thread(() -> {
            while (running) {
                try {
                    String text = Notifier.instance().take().text();
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    SearchController currentController = controller;
                    if (currentController == null) {
                        continue;
                    }
                    Platform.runLater(() -> currentController.addNotification(text));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "NotificationUIBridge-Thread");
        notificationThread.setDaemon(true);
        notificationThread.start();
    }

    public void stop() {
        running = false;
        if (notificationThread != null) {
            notificationThread.interrupt();
            notificationThread = null;
        }
        Notifier.shutdownLogger();
    }
}