package rt.model.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dialogue {

    private final String model;
    private final List<AiChatMessage> aiChatMessages;
    private final List<Tool> tools;
    private final double temperature;
    private final boolean stream;

    private Dialogue(Builder builder) {
        this.model = builder.model;
        this.aiChatMessages = builder.aiChatMessages;
        this.tools = builder.tools;
        this.temperature = builder.temperature;
        this.stream = builder.stream;
    }

    public void addSystemMessage(String content) {
        aiChatMessages.add(new AiChatMessage("system", content));
    }

    public void addUserMessage(String content) {
        aiChatMessages.add(new AiChatMessage("user", content));
    }

    public void addAssistantMessage(String content) {
        aiChatMessages.add(new AiChatMessage("assistant", content));
    }

    public void addAssistantToolCalls(String content, List<ToolCall> toolCalls) {
        aiChatMessages.add(new AiChatMessage("assistant", content, toolCalls, null));
    }

    public void addToolMessage(String toolCallId, String content) {
        aiChatMessages.add(new AiChatMessage("tool", content, null, toolCallId));
    }

    public Tool getTool(String name) {
        return tools.stream()
                .filter(tool -> tool.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public String getModel() {
        return model;
    }

    public List<AiChatMessage> getMessages() {
        return aiChatMessages;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public double getTemperature() {
        return temperature;
    }

    public boolean getStream() {
        return stream;
    }

    public void clearChat() {
        aiChatMessages.removeIf(m -> !"system".equals(m.role()));
    }

    public static class Builder {
        private String model;
        private final List<AiChatMessage> aiChatMessages = new ArrayList<>();
        private final List<Tool> tools = new ArrayList<>();
        private double temperature = 0.4;
        private boolean stream = false;

        public Builder setModel(String model) {
            this.model = model;
            return this;
        }

        public Builder addTool(Tool tool) {
            if (tool != null) {
                tools.add(tool);
            }
            return this;
        }

        public Builder addTools(List<Tool> tools) {
            if (tools != null && !tools.isEmpty()) {
                this.tools.addAll(tools);
            }
            return this;
        }

        public Builder addSystemMessage(String content) {
            aiChatMessages.add(new AiChatMessage("system", content));
            return this;
        }

        public Builder addUserMessage(String content) {
            aiChatMessages.add(new AiChatMessage("user", content));
            return this;
        }

        public Builder addAssistantMessage(String content) {
            aiChatMessages.add(new AiChatMessage("assistant", content));
            return this;
        }

        public Builder setTemperature(double temperature) {
            this.temperature = Math.clamp(temperature, 0.0, 2.0);
            return this;
        }

        public Builder setStream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public Dialogue build() {
            return new Dialogue(this);
        }
    }
}