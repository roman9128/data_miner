package rt.ai;

import rt.model.ai.Tool;
import rt.model.document.InfoToShow;
import rt.storage.DatabaseManager;
import rt.common_utils.Text;

import java.util.List;

class LastSearchTool implements Tool {

    private final DatabaseManager db;

    LastSearchTool(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public String getName() {
        return Constants.LAST_SEARCH_TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return Constants.LAST_SEARCH_TOOL_DESC.formatted(Constants.MAX_MESSAGES);
    }

    @Override
    public String getParameters() {
        return Constants.LAST_SEARCH_TOOL_PARAMS;
    }

    @Override
    public String execute(String arguments) {
        List<InfoToShow> messages = db.getLastMessages(Constants.MAX_MESSAGES);
        if (messages.isEmpty()) return "";
        return Text.format(messages);
    }
}
