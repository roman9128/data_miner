package rt.model.ai;

import java.util.List;

public record AiChatMessage(
        String role,
        String content,
        List<ToolCall> toolCalls,
        String toolCallId
) {
    public AiChatMessage(String role, String content) {
        this(role, content, null, null);
    }
}