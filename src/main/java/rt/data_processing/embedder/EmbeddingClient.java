package rt.data_processing.embedder;

import rt.api.ExternalAPI;
import rt.model.notification.Notification;
import rt.notifier.Notifier;

import java.util.List;

public class EmbeddingClient {

    public float[] createEmbedding(String text) {
        if (text == null || text.isBlank()) return new float[0];

        try {
            float[][] embeddings = ExternalAPI.getEmbeddings(List.of(text));
            if (embeddings.length == 0) {
                return new float[0];
            }
            return embeddings[0];
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Ошибка создания embedding: " + e);
            return new float[0];
        }
    }

    public float[][] createEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) return new float[0][];

        try {
            return ExternalAPI.getEmbeddings(texts);
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Ошибка создания embedding: " + e);
            return new float[0][];
        }
    }
}