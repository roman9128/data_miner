package rt.ai;

import rt.data_processing.noun_extractor.NounExtractor;
import rt.model.ai.QueryContext;
import rt.model.ai.Tool;
import rt.model.message.InfoToShow;
import rt.model.noun.Noun;
import rt.storage.DatabaseManager;
import rt.utils.DateTimeUtils;
import rt.utils.TextUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class ExactSearchTool implements Tool {

    private final DatabaseManager db;
    private final NounExtractor nounExtractor;
    private QueryContext queryContext;

    public ExactSearchTool(DatabaseManager db, NounExtractor nounExtractor) {
        this.db = db;
        this.nounExtractor = nounExtractor;
    }

    @Override
    public String getName() {
        return Constants.EXACT_SEARCH_TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return Constants.EXACT_SEARCH_TOOL_DESC.formatted(Constants.MAX_MESSAGES);
    }

    @Override
    public String getParameters() {
        return Constants.EXACT_SEARCH_TOOL_PARAMS;
    }

    @Override
    public String execute(String arguments) {
        if (arguments.isBlank()) return "";
        List<Noun> nouns = nounExtractor.extract(arguments);
        List<InfoToShow> messagesFromExactSearch = db.searchMessagesExact(arguments, queryContext, Constants.MAX_MESSAGES);
        List<InfoToShow> messagesFromExactSearchOfStems = db.searchMessagesExact(TextUtils.stem(arguments), queryContext, Constants.MAX_MESSAGES);
        List<InfoToShow> messagesFromNounSearch = db.searchMessagesByNouns(nouns, queryContext, Constants.MAX_MESSAGES);
        List<InfoToShow> messagesFromEntitySearch = db.searchMessagesByEntities(arguments, queryContext, Constants.MAX_MESSAGES);
        List<InfoToShow> messages = Stream
                .of(messagesFromExactSearch, messagesFromExactSearchOfStems, messagesFromNounSearch, messagesFromEntitySearch)
                .flatMap(List::stream)
                .distinct()
                .sorted(Comparator.comparing((InfoToShow m) -> DateTimeUtils.getLocalDateTimeOf(m.publishedAt())).reversed())
                .limit(Constants.MAX_MESSAGES).toList();
        if (messages.isEmpty()) return "";
        return TextUtils.format(messages);
    }

    @Override
    public void setQueryContext(QueryContext queryContext) {
        this.queryContext = queryContext;
    }
}
