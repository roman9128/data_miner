package rt.common;

import rt.common.entities_and_dtos.Notification;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Notifier {
    private static volatile Notifier instance;
    private static final Logger logger = Logger.getLogger("Notifier");
    private final LinkedBlockingQueue<Notification> queue = new LinkedBlockingQueue<>();

    static {
        try {
            FileHandler fileHandler = new FileHandler("application.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Notifier() {
    }

    public static Notifier instance() {
        if (instance == null) {
            synchronized (Notifier.class) {
                if (instance == null) {
                    instance = new Notifier();
                }
            }
        }
        return instance;
    }

    public void add(Notification.Level level, String text) {
        logger.info(text);
        if (level == Notification.Level.ONLY_TO_LOG) {
            return;
        }
        queue.offer(new Notification(level, text));
    }

    public Notification take() throws InterruptedException {
        return queue.take();
    }

    public static void shutdownLogger() {
        for (Handler handler : logger.getHandlers()) {
            handler.close();
        }
    }
}