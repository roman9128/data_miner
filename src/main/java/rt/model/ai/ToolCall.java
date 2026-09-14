package rt.model.ai;

public record ToolCall(
        String id,
        String name,
        String arguments
) {
}