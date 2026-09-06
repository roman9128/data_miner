package rt.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import rt.common.entities_and_dtos.Noun;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExternalAPIHandler {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String lemmas = "http://localhost:8002/lemmas";
    private final String analyze = "http://localhost:8002/analyze";

    public List<Noun> getNouns(String text) throws IOException, InterruptedException {

        String requestJson = objectMapper.writeValueAsString(Map.of("text", text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(analyze))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response);

        return parseNounsResponse(response.body());
    }

    private List<Noun> parseNounsResponse(String json) throws IOException {
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

    public List<String> getLemmas(String text) throws IOException, InterruptedException {

        String requestJson = objectMapper.writeValueAsString(Map.of("text", text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(lemmas))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response);

        return parseLemmasResponse(response.body());
    }

    private List<String> parseLemmasResponse(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        List<String> result = new ArrayList<>();

        JsonNode lemmas = root.get("lemmas");
        if (lemmas != null && lemmas.isArray()) {
            for (JsonNode lemma : lemmas) {
                result.add(lemma.asText());
            }
        }
        return result;
    }

    private void checkStatus(HttpResponse<String> response) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Natasha returned HTTP " + response.statusCode() + ": " + response.body());
        }
    }
}