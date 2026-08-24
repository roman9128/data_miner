package rt.common.entities_and_dtos;

public record MessageTies(
        long replyToChatId,
        long replyToMessageId,
        long forwardOriginChatId,
        long forwardOriginMessageId
) {
}
