package rt.core;

import rt.model.ai.Usage;

public interface AssistantAgent {
    void sendAnswer(String answer);
    void sendUsage(Usage usage);
}
