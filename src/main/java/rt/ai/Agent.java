package rt.ai;

import rt.api.ExternalAPIHandler;
import rt.data.embedder.EmbeddingClient;
import rt.data.storage.SQLiteDB;
import rt.model.ai.*;

import java.io.IOException;
import java.util.List;

public class Agent {

    private final ExternalAPIHandler api;
    private Dialogue dialogue;
    private static final int MAX_ITERATIONS = 10;

    public Agent(ExternalAPIHandler api, SQLiteDB db, EmbeddingClient embeddingClient) {
        this.api = api;

        List<Tool> availableTools = List.of(new SearchMessagesTool(db, embeddingClient));

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

    public String ask(String question) throws IOException, InterruptedException {
        int iteration = 0;
        dialogue.addUserMessage(question);
        while (iteration < MAX_ITERATIONS) {
            iteration++;
            dialogue = api.chat(dialogue);
            AiChatMessage aiChatMessage = dialogue.getMessages().getLast();
            if (aiChatMessage.toolCalls() == null || aiChatMessage.toolCalls().isEmpty()) {
                return aiChatMessage.content();
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
        return "Ничего не найдено";
    }

    public void clearChat() {
        dialogue.clearChat();
    }
}