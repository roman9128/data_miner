package rt.model.ai;

public class Usage {

    int promptTokens;
    int completionTokens;
    int totalTokens;

    public Usage(int promptTokens, int completionTokens, int totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public void add(Usage usage) {
        this.promptTokens += usage.promptTokens;
        this.completionTokens += usage.completionTokens;
        this.totalTokens += usage.totalTokens;
    }

    @Override
    public String toString() {
        return "↑%d  ↓%d  ↑↓%d".formatted(this.promptTokens, this.completionTokens, this.totalTokens);
    }
}
