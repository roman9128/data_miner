package rt.ai;

import rt.data_processing.embedder.EmbeddingClient;
import rt.model.ai.Tool;
import rt.model.document.InfoToShow;
import rt.storage.DatabaseManager;
import rt.common_utils.TextUtils;
import rt.common_utils.VectorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticSearchTool implements Tool {

    private final DatabaseManager db;
    private final EmbeddingClient embeddingClient;

    public SemanticSearchTool(DatabaseManager db, EmbeddingClient embeddingClient) {
        this.db = db;
        this.embeddingClient = embeddingClient;
    }

    @Override
    public String getName() {
        return Constants.SEMANTIC_SEARCH_TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return Constants.SEMANTIC_SEARCH_TOOL_DESC.formatted(Constants.MAX_MESSAGES);
    }

    @Override
    public String getParameters() {
        return Constants.SEMANTIC_SEARCH_TOOL_PARAMS;
    }

    @Override
    public String execute(String arguments) {
        if (arguments.isBlank()) return "";
        List<InfoToShow> messages = findBySemantic(arguments);
        if (messages.isEmpty()) return "";
        return TextUtils.format(messages);
    }

    private List<InfoToShow> findBySemantic(String query) {
        float[] queryEmb = embeddingClient.createEmbedding(query);
        if (queryEmb.length == 0) return List.of();
        Map<Long, float[]> messagesEmb = db.getMessageIdsAndEmbeddings();
        Map<Long, Double> candidatesMessageIds = new HashMap<>();
        for (Map.Entry<Long, float[]> entry : messagesEmb.entrySet()) {
            double similarity = VectorUtils.cosineSimilarity(queryEmb, entry.getValue());
            if (similarity >= 0.55) candidatesMessageIds.put(entry.getKey(), similarity);
        }
        if (candidatesMessageIds.isEmpty()) return List.of();
        List<Long> bestSimilarityMessageIds = getBestSimilarityMessageIds(candidatesMessageIds);
        return db.getMessagesByIds(bestSimilarityMessageIds);
    }

    private List<Long> getBestSimilarityMessageIds(Map<Long, Double> candidatesMessageIds) {
        List<Map.Entry<Long, Double>> list = candidatesMessageIds.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .toList();
        List<Long> result = new ArrayList<>();
        result.add(list.getFirst().getKey());
        for (int i = 1; i < list.size(); i++) {
            double previous = list.get(i - 1).getValue();
            double current = list.get(i).getValue();
            double max = list.getFirst().getValue();
            if (previous - current > 0.04) break;
            if (max - current >= 0.15) break;
            if (result.size() >= Constants.MAX_MESSAGES) break;
            result.add(list.get(i).getKey());
        }
        return result;
    }
}