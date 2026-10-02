package rt.core;

import rt.model.ai.Usage;

public interface AgentAssistant {
    void sendAnswer(String answer);
    void sendUsage(Usage usage);
}
