package rt.core;

import rt.model.notification.Notification;
import rt.notifier.Notifier;
import rt.storage.DatabaseManager;

public class UseCaseCsvExport {

    private final Core core;
    private final DatabaseManager db;

    private volatile boolean isExporting;

    UseCaseCsvExport(Core core, DatabaseManager db) {
        this.core = core;
        this.db = db;
    }

    public void exportToCsv() {
        if (core.isBusy()) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Сейчас немного занят, подождите");
            return;
        }
        Notifier.instance().add(Notification.Level.SHOW_USER, "Начинаю экспорт базы данных");
        isExporting = true;
        db.exportToCsv();
        isExporting = false;
        Notifier.instance().add(Notification.Level.SHOW_USER, "Экспорт завершён");
    }

    public boolean isExporting() {
        return isExporting;
    }
}