package rt.model.message;

public record MessageTypeText(
        MessageContentType type,
        String text
) {
}
