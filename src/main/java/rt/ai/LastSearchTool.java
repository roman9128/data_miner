package rt.ai;

import rt.model.ai.QueryContext;
import rt.model.ai.Tool;
import rt.model.message.InfoToShow;
import rt.storage.DatabaseManager;
import rt.utils.TextUtils;

import java.util.List;

public class LastSearchTool implements Tool {

    private final DatabaseManager db;
    private QueryContext queryContext;

    public LastSearchTool(DatabaseManager db) {
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
        List<InfoToShow> messages = db.getLastMessages(queryContext, Constants.MAX_MESSAGES);
        if (messages.isEmpty()) return "";
        return TextUtils.format(messages);
    }

    @Override
    public void setQueryContext(QueryContext queryContext) {
        this.queryContext = queryContext;
    }
}
