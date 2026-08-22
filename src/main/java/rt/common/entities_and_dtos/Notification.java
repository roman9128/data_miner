package rt.common.entities_and_dtos;

public record Notification(Level level, String text) {

    public enum Level {
        ONLY_TO_LOG,
        SHOW_USER
    }
}