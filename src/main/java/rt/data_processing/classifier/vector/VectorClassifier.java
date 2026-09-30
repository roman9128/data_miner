package rt.data_processing.classifier.vector;

import rt.data_processing.embedder.EmbeddingClient;
import rt.utils.VectorUtils;

import java.util.*;

public class VectorClassifier {
    private final EmbeddingClient embeddingClient;
    private final List<Topic> topics = new ArrayList<>();
    private boolean isInitialized = false;

    public VectorClassifier(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    public Map<String, Double> classify(float[] textEmb) {
        if (!isInitialized) initializeTopics();
        if (topics.isEmpty()) return Map.of();

        Map<String, Double> result = new HashMap<>();
        for (Topic topic : topics) {
            double similarity = findSimilarity2(textEmb, topic.centroid(), topic.referenceEmbeddings());
            if (similarity >= 0.6) result.put(topic.label(), similarity * 100);
        }
        return result;
    }

    private double findSimilarity(float[] textEmb, float[] centroid, float[][] referenceEmbeddings) {
        List<Double> similarities = new ArrayList<>();

        double centroidSimilarity = VectorUtils.cosineSimilarity(textEmb, centroid);
        if (centroidSimilarity < 0.55) return 0.0;

        for (float[] reference : referenceEmbeddings) {
            double similarity = VectorUtils.cosineSimilarity(textEmb, reference);
            similarities.add(similarity);
        }
        int referenceMatchCount = 0;
        for (Double similarity : similarities) {
            if (similarity > 0.68) referenceMatchCount++;
        }
        double referenceMatchShare = (double) referenceMatchCount / similarities.size();
        double shareLimit = Math.clamp(referenceMatchShare, 0.3, 0.7);

        return referenceMatchShare * (1 - shareLimit) + centroidSimilarity * shareLimit;
    }

    private double findSimilarity2(float[] textEmb, float[] centroid, float[][] referenceEmbeddings) {
        List<Double> similarities = new ArrayList<>();

        double centroidSimilarity = VectorUtils.cosineSimilarity(textEmb, centroid);
        if (centroidSimilarity < 0.6) return 0.0;

        for (float[] reference : referenceEmbeddings) {
            double similarity = VectorUtils.cosineSimilarity(textEmb, reference);
            similarities.add(similarity);
        }

        return similarities.stream()
                .mapToDouble(d -> {
                    if (d < 0.55) return 0;
                    else if (d > 0.7) return 1;
                    else return d * 0.75;
                }).average().orElse(0);
    }

    private void initializeTopics() {
        Map<String, List<String>> topicReferencesMap = Map.of(
                References.AI_LABEL, References.AI_TEXTS,
                References.IT_LABEL, References.IT_TEXTS,
                References.SPORT_LABEL, References.SPORT_TEXTS
        );

        for (Map.Entry<String, List<String>> entry : topicReferencesMap.entrySet()) {
            float[][] referenceEmbeddings = embeddingClient.createEmbeddings(entry.getValue());
            float[] topicCentroid = VectorUtils.computeCentroid(referenceEmbeddings);
            topics.add(new Topic(entry.getKey(), topicCentroid, referenceEmbeddings));
        }
        isInitialized = true;
    }
}