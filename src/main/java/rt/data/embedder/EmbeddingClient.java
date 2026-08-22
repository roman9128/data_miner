package rt.data.embedder;

import com.fasterxml.jackson.databind.ObjectMapper;
import rt.common.Notifier;
import rt.common.entities_and_dtos.Notification;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class EmbeddingClient {

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String url = "http://localhost:8080/embed";

    public EmbeddingClient(HttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public float[] createEmbedding(String text) {
        if (text == null || text.isBlank()) return new float[0];

        try {
            float[][] embeddings = requestServerForEmbedding(List.of(text));
            if (embeddings.length == 0) {
                return new float[0];
            }
            return embeddings[0];

        } catch (Exception e) {
            Notifier.instance().add(
                    Notification.Level.ONLY_TO_LOG,
                    "Ошибка создания embedding: " + e
            );
            return new float[0];
        }
    }

    private float[][] requestServerForEmbedding(List<String> texts) throws IOException, InterruptedException {

        String json = mapper.writeValueAsString(Map.of("inputs", texts));
        HttpResponse<String> response = getResponse(json);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "Embedding server returned HTTP "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        try {
            return mapper.readValue(response.body(), float[][].class);
        } catch (Exception e) {
            throw new IOException("Не удалось разобрать ответ embedding-сервера: " + response.body(), e);
        }
    }

    private HttpResponse<String> getResponse(String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }
}