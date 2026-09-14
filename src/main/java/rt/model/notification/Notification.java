package rt.model.notification;

public record Notification(Level level, String text) {

    public enum Level {
        ONLY_TO_LOG,
        SHOW_USER
    }
}