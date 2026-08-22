package rt.data.noun_extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import rt.common.Notifier;
import rt.common.entities_and_dtos.Notification;
import rt.common.entities_and_dtos.Noun;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NounExtractor {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String url = "http://localhost:8002/keywords";

    public NounExtractor(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public List<Noun> extract(String text) {
        try {
            return extractFrom(text);
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.toString());
            return List.of();
        }
    }

    private List<Noun> extractFrom(String text) throws IOException, InterruptedException {

        String requestJson = objectMapper.writeValueAsString(Map.of("text", text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Natasha returned HTTP " + response.statusCode() + ": " + response.body());
        }
        return parseResponse(response.body());
    }

    private List<Noun> parseResponse(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        List<Noun> result = new ArrayList<>();

        JsonNode nouns = root.get("nouns");
        if (nouns != null && nouns.isArray()) {
            for (JsonNode noun : nouns) {
                String lemma = noun.path("lemma").asText();
                int count = noun.path("count").asInt();
                result.add(
                        new Noun(
                                lemma,
                                count
                        )
                );
            }
        }
        return result;
    }
}