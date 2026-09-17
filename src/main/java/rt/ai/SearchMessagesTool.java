package rt.ai;

import rt.data.embedder.EmbeddingClient;
import rt.data.embedder.VectorUtils;
import rt.data.storage.SQLiteDB;
import rt.model.ai.QueryContext;
import rt.model.ai.Tool;
import rt.model.message.InfoToShow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchMessagesTool implements Tool {

    private final SQLiteDB db;
    private final EmbeddingClient embeddingClient;
    private QueryContext queryContext;

    public SearchMessagesTool(SQLiteDB db, EmbeddingClient embeddingClient) {
        this.db = db;
        this.embeddingClient = embeddingClient;
    }

    @Override
    public String getName() {
        return "search_messages";
    }

    @Override
    public String getDescription() {
        return """
                Search messages stored in the local database.
                The query is searched by semantic similarity using embeddings.
                Provide a detailed and specific natural-language query describing the information you want to find.
                Include relevant context, names, terms, events, actions, dates, and other useful details when available.
                More detailed queries generally improve semantic search quality.
                Use this tool when you need to find messages relevant to the user's request.
                The query should clearly express the user's search intent and include the key information to find.
                Semantic search is available only for Russian-language content.
                The tool returns a list of matching messages with their link, chat name, text, and date of publishing.
                
                IMPORTANT:
                Never use semantic search to retrieve the entire database.
                Semantic search is for finding messages relevant to a specific information need.
                For requests about the database as a whole, use get_database_stats only.
                """;
    }

    @Override
    public String getParameters() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "query": {
                      "type": "string",
                      "description": "A detailed and specific natural-language description of the information to find. Include relevant context, names, terms, events, actions, dates, or other details when available. More detailed queries improve semantic search quality."
                    }
                  },
                  "required": ["query"]
                }
                """;
    }

    @Override
    public String execute(String arguments) {
        List<InfoToShow> messagesFoundBySemantic = findBySemantic(arguments);
        if (messagesFoundBySemantic.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (InfoToShow m : messagesFoundBySemantic) {
            sb
                    .append("<").append(System.lineSeparator())
                    .append(m.chatName()).append(System.lineSeparator())
                    .append(m.publishedAt()).append(System.lineSeparator())
                    .append(m.link()).append(System.lineSeparator())
                    .append(m.text()).append(System.lineSeparator())
                    .append(">").append(System.lineSeparator());
        }
        return sb.toString();
    }

    private List<InfoToShow> findBySemantic(String query) {
        float[] queryEmb = embeddingClient.createEmbedding(query);
        if (queryEmb.length == 0) return List.of();

        Map<Long, float[]> messagesEmb = db.getMessageIdsAndEmbeddings(queryContext);
        Map<Long, Double> highSimilarityMessageIds = new LinkedHashMap<>();
        Map<Long, Double> mediumSimilarityMessageIds = new LinkedHashMap<>();

        for (Map.Entry<Long, float[]> entry : messagesEmb.entrySet()) {
            double similarity = VectorUtils.cosineSimilarity(queryEmb, entry.getValue());
            if (similarity >= 0.75) highSimilarityMessageIds.put(entry.getKey(), similarity);
            else if (similarity >= 0.5) mediumSimilarityMessageIds.put(entry.getKey(), similarity);
        }

        int minimumResults = 20;
        if (highSimilarityMessageIds.size() >= minimumResults)
            return db.getMessagesByIds(highSimilarityMessageIds.keySet());

        return db.getMessagesByIds(
                mergeAndSortByValueWithLimit(
                        highSimilarityMessageIds,
                        mediumSimilarityMessageIds,
                        minimumResults).keySet()
        );
    }

    private <K, V extends Comparable<? super V>> LinkedHashMap<K, V> mergeAndSortByValueWithLimit(Map<K, V> map1, Map<K, V> map2, int limit) {
        return Stream.concat(map1.entrySet().stream(), map2.entrySet().stream())
                .sorted(Map.Entry.<K, V>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public void setQueryContext(QueryContext queryContext) {
        this.queryContext = queryContext;
    }
}