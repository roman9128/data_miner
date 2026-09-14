package rt.model.message;

public record MessageTies(
        long replyToChatId,
        long replyToMessageId,
        long forwardOriginChatId,
        long forwardOriginMessageId
) {
}
