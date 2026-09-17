package rt.api;

import rt.core.Notifier;
import rt.model.ai.Dialogue;
import rt.model.ai.ToolCall;
import rt.model.notification.Notification;
import rt.model.noun.Noun;
import rt.utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ExternalAPIHandler {

    private final HttpClient client = HttpClient.newHttpClient();
    private final String EMBED = "http://127.0.0.1:8001/embed";
    private final String HEALTH_EMBED = "http://127.0.0.1:8001/health";
    private final String LEMMAS = "http://127.0.0.1:8002/lemmas";
    private final String NOUNS = "http://127.0.0.1:8002/analyze";
    private final String HEALTH_NOUNS = "http://127.0.0.1:8002/health";
    private final String AI = "http://192.168.0.33:11434/v1/chat/completions";

    public boolean checkNounExtractorsHealth() {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(HEALTH_NOUNS)).GET().build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, e.toString());
            return false;
        }
    }

    public boolean checkEmbeddingServicesHealth() {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(HEALTH_EMBED)).GET().build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, e.toString());
            return false;
        }
    }

    public List<Noun> getNouns(String text) throws IOException, InterruptedException {
        String json = JsonUtils.makeJson(Map.of("text", text));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NOUNS))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response, "Natasha");
        return JsonUtils.parseNounsResponse(response.body());
    }

    public List<String> getLemmas(String text) throws IOException, InterruptedException {
        String json = JsonUtils.makeJson(Map.of("text", text));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LEMMAS))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response, "Natasha");
        return JsonUtils.parseLemmasResponse(response.body());
    }

    public float[][] getEmbeddings(List<String> texts) throws IOException, InterruptedException {
        String json = JsonUtils.makeJson(Map.of("inputs", texts));
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EMBED))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkStatus(response, "Embedding server");
        return JsonUtils.parseEmbeddingsResponse(response.body());
    }

    public Dialogue chat(Dialogue dialogue) throws IOException, InterruptedException {
        String json = JsonUtils.makeJson(dialogue);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AI))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response, "AI");
        String content = JsonUtils.getAiResponse(response.body());
        List<ToolCall> toolCalls = JsonUtils.parseToolCalls(response.body());
        if (!toolCalls.isEmpty()) {
            dialogue.addAssistantToolCalls(content, toolCalls);
        } else {
            dialogue.addAssistantMessage(content);
        }
        return dialogue;
    }

    private void checkStatus(HttpResponse<String> response, String serviceName) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(serviceName + " returned HTTP " + response.statusCode() + ": " + response.body());
        }
    }
}