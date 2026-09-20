package rt.data_processing.embedder;

import rt.api.ExternalAPIHandler;
import rt.core.Notifier;
import rt.model.notification.Notification;

import java.util.List;

public class EmbeddingClient {

    private final ExternalAPIHandler externalAPIHandler;

    public EmbeddingClient(ExternalAPIHandler externalAPIHandler) {
        this.externalAPIHandler = externalAPIHandler;
    }

    public float[] createEmbedding(String text) {
        if (text == null || text.isBlank()) return new float[0];

        try {
            float[][] embeddings = externalAPIHandler.getEmbeddings(List.of(text));
            if (embeddings.length == 0) {
                return new float[0];
            }
            return embeddings[0];
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Ошибка создания embedding: " + e);
            return new float[0];
        }
    }
}