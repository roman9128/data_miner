package rt.view.main;

import javafx.application.Platform;
import rt.core.Notifier;

public class NotificationHandler {

    private volatile boolean running = false;
    private Thread notificationThread;
    private MainController controller;

    NotificationHandler() {
    }

    void setController(MainController controller) {
        this.controller = controller;
    }

    void start() {
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
                    MainController currentController = controller;
                    if (currentController == null) {
                        continue;
                    }
                    Platform.runLater(() -> currentController.addNotification(text));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "NotificationHandler-Thread");
        notificationThread.setDaemon(true);
        notificationThread.start();
    }

    void stop() {
        running = false;
        if (notificationThread != null) {
            notificationThread.interrupt();
            notificationThread = null;
        }
        Notifier.shutdownLogger();
    }
}