package rt.ai;

import rt.model.ai.Tool;
import rt.model.document.InfoToShow;
import rt.storage.DatabaseManager;
import rt.common_utils.TextUtils;

import java.util.List;

public class TopicSearchTool implements Tool {

    private final DatabaseManager db;

    public TopicSearchTool(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public String getName() {
        return Constants.TOPIC_SEARCH_TOOL_NAME;
    }

    @Override
    public String getDescription() {
        String availableTopics = String.join(", ", db.getDatabaseContext().topics());
        return Constants.TOPIC_SEARCH_TOOL_DESC.formatted(availableTopics, Constants.MAX_MESSAGES);
    }

    @Override
    public String getParameters() {
        return Constants.TOPIC_SEARCH_TOOL_PARAMS;
    }

    @Override
    public String execute(String arguments) {
        if (arguments.isBlank()) return "";
        List<InfoToShow> messages = db.searchMessagesByTopic(arguments, Constants.MAX_MESSAGES);
        if (messages.isEmpty()) return "";
        return TextUtils.format(messages);
    }
}
