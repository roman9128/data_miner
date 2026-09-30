package rt.model.ai;

public record Usage(
        int promptTokens,
        int completionTokens,
        int totalTokens
) {
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("prompt: ").append(promptTokens);
        sb.append(", completion: ").append(completionTokens);
        sb.append(", total: ").append(totalTokens);
        return sb.toString();
    }
}
