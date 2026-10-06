package rt.data_processing.classifier.vector;

import rt.notifier.Notifier;
import rt.data_processing.embedder.EmbeddingClient;
import rt.common_utils.Vector;
import rt.model.notification.Notification;

import java.util.*;

public class VectorClassifier {
    private final EmbeddingClient embeddingClient;
    private final List<Topic> topics = new ArrayList<>();
    private boolean isInitialized = false;

    private static final double MIN_SIMILARITY = 0.55;
    private static final double GOOD_SIMILARITY = 0.7;

    public VectorClassifier(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    public Map<String, Double> classify(float[] textEmb) {
        if (!isInitialized) initializeTopics();
        if (topics.isEmpty()) return Map.of();

        Map<String, Double> result = new HashMap<>();
        for (Topic topic : topics) {
            double similarity = findSimilarity(textEmb, topic.centroid(), topic.referenceEmbeddings());
            if (similarity >= 0.6) result.put(topic.label(), similarity * 100);
        }
        return result;
    }

    private double findSimilarity(float[] textEmb, float[] centroid, float[][] referenceEmbeddings) {
        if (referenceEmbeddings.length == 0) return 0.0;
        if (Vector.cosineSimilarity(textEmb, centroid) < 0.6) return 0.0;

        double sum = 0.0;

        for (float[] reference : referenceEmbeddings) {
            double similarity = Vector.cosineSimilarity(textEmb, reference);
            double score = Math.clamp((similarity - MIN_SIMILARITY) / (GOOD_SIMILARITY - MIN_SIMILARITY), 0.0, 1.0);
            sum += score * score;
        }

        return sum / referenceEmbeddings.length;
    }

    private void initializeTopics() {
        Map<String, List<String>> topicReferencesMap = Map.of(
                References.AI_LABEL, References.AI_TEXTS,
                References.IT_LABEL, References.IT_TEXTS,
                References.SPORT_LABEL, References.SPORT_TEXTS
        );

        for (Map.Entry<String, List<String>> entry : topicReferencesMap.entrySet()) {
            try {
                float[][] referenceEmbeddings = embeddingClient.createEmbeddings(entry.getValue());
                float[] topicCentroid = Vector.computeCentroid(referenceEmbeddings);
                topics.add(new Topic(entry.getKey(), topicCentroid, referenceEmbeddings));
            } catch (Exception e) {
                Notifier.instance().add(
                        Notification.Level.SHOW_USER,
                        "Ошибка для темы %s: ".formatted(entry.getKey()) + e.getMessage()
                );
            }
        }
        isInitialized = true;
    }
}