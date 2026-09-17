package rt.ai;

import rt.api.ExternalAPIHandler;
import rt.core.AgentAssistant;
import rt.data.embedder.EmbeddingClient;
import rt.data.storage.SQLiteDB;
import rt.model.ai.*;

import java.io.IOException;
import java.util.List;

public class Agent {

    private final AgentAssistant assistant;
    private final ExternalAPIHandler api;
    private Dialogue dialogue;
    private final List<Tool> availableTools;
    private static final int MAX_ITERATIONS = 10;
    private boolean isThinking;

    public Agent(ExternalAPIHandler api, SQLiteDB db, EmbeddingClient embeddingClient, AgentAssistant assistant) {
        this.assistant = assistant;
        this.api = api;
        this.availableTools = List.of(
                new SearchMessagesTool(db, embeddingClient),
                new DatabaseStatsTool(db)
        );
        this.dialogue = new Dialogue.Builder()
                .setModel(Model.GPT_OSS)
                .addSystemMessage("""
                        You answer questions based on a text database.
                        Use tools when the answer requires information from the database.
                        Do not invent facts that are not supported by the database or conversation.
                        Use multiple tool calls when necessary to answer a complex question.
                        Analyze the retrieved information and answer the user's original question.
                        If the required information cannot be found, say so clearly.
                        """)
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
                dialogue = api.chat(dialogue);
            } catch (IOException | InterruptedException e) {
                answer("Не удалось обработать Ваш запрос: " + e);
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
                String result = tool.execute(call.arguments());
                dialogue.addToolMessage(call.id(), result);
            }
        }
        dialogue.addAssistantMessage("Ничего не найдено");
        answer("Ничего не найдено");
    }

    private void answer(String answer) {
        isThinking = false;
        assistant.sendAnswer(answer);
    }

    public void clearChat() {
        dialogue.clearChat();
    }

    public boolean isThinking(){
        return isThinking;
    }

    public void setQueryContext(QueryContext queryContext) {
        availableTools.forEach(t -> t.setQueryContext(queryContext));
    }
}