package rt.model.ai;

public enum Model {
    GPT_OSS("gpt-oss:120b-cloud"),
    QWEN2("qwen2.5:7b"),
    QWEN3("qwen3:8b");

    private final String name;

    Model(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}