package rt.ai;

import rt.api.ExternalAPIHandler;
import rt.config.AiProperties;
import rt.core.AssistantAgent;
import rt.notifier.Notifier;
import rt.data_processing.embedder.EmbeddingClient;
import rt.data_processing.noun_extractor.NounExtractor;
import rt.model.notification.Notification;
import rt.storage.DatabaseManager;
import rt.model.ai.*;
import rt.common_utils.JsonUtils;

import java.io.IOException;
import java.util.List;

public class Agent {

    private final AssistantAgent assistant;
    private final ExternalAPIHandler api;
    private final Dialogue dialogue;
    private final Usage usage = new Usage(0, 0, 0);
    private final List<Tool> availableTools;
    private static final int MAX_ITERATIONS = 10;
    private volatile boolean isThinking;

    public Agent(ExternalAPIHandler api, DatabaseManager db, EmbeddingClient embeddingClient, NounExtractor nounExtractor, AssistantAgent assistant) {
        this.assistant = assistant;
        this.api = api;
        this.availableTools = List.of(
                new LastSearchTool(db),
                new ExactSearchTool(db, nounExtractor),
                new SemanticSearchTool(db, embeddingClient),
                new TopicSearchTool(db),
                new DatabaseStatsTool(db)
        );
        this.dialogue = new Dialogue.Builder()
                .setModel(AiProperties.getModel())
                .addSystemMessage(Constants.INITIAL_SYSTEM_MESSAGE.formatted(MAX_ITERATIONS))
                .addTools(availableTools)
                .build();
    }

    public void ask(String question) {
        if (isThinking) return;
        isThinking = true;
        int iteration = 0;
        dialogue.addUserMessage(question);
        while (iteration < MAX_ITERATIONS) {
            iteration++;
            try {
                api.chat(dialogue, usage);
            } catch (IOException | InterruptedException e) {
                String errMsg = "Не удалось обработать Ваш запрос: " + e;
                answer(errMsg);
                Notifier.instance().add(Notification.Level.ONLY_TO_LOG, errMsg);
                return;
            }
            AiChatMessage aiChatMessage = dialogue.getMessages().getLast();
            if (aiChatMessage.toolCalls() == null || aiChatMessage.toolCalls().isEmpty()) {
                answer(aiChatMessage.content());
                return;
            }
            for (ToolCall call : aiChatMessage.toolCalls()) {
                Tool tool = dialogue.getTool(call.name());
                if (tool == null) {
                    dialogue.addToolMessage(call.id(), "Tool '%s' is unavailable. Do not call it again.".formatted(call.name()));
                    continue;
                }
                String result = tool.execute(JsonUtils.getQuery(call.arguments()));
                dialogue.addToolMessage(call.id(), result);
            }
        }
        dialogue.addAssistantMessage("Ничего не найдено");
        answer("Ничего не найдено");
    }

    private void answer(String answer) {
        isThinking = false;
        assistant.sendAnswer(answer);
        assistant.sendUsage(usage);
    }

    public void clearChat() {
        dialogue.clearChat();
    }

    public boolean isThinking() {
        return isThinking;
    }
}