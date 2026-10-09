package rt.api;

import rt.notifier.Notifier;
import rt.config.AiProperties;
import rt.model.ai.Dialogue;
import rt.model.ai.ToolCall;
import rt.model.ai.Usage;
import rt.model.notification.Notification;
import rt.model.noun.Noun;
import rt.common_utils.Json;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class ExternalAPI {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String HEALTH = "http://127.0.0.1:8001/health";
    private static final String EMBED = "http://127.0.0.1:8001/embed";
    private static final String NOUNS = "http://127.0.0.1:8001/nouns";

    private ExternalAPI() {
    }

    public static boolean checkHealth() {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(HEALTH)).GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, e.toString());
            return false;
        }
    }

    public static List<Noun> getNouns(String text) throws IOException, InterruptedException {
        String json = Json.makeJson(Map.of("text", text));
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NOUNS))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkStatus(response, "Natasha");
        return Json.parseNounsResponse(response.body());
    }

    public static float[][] getEmbeddings(List<String> texts) throws IOException, InterruptedException {
        String json = Json.makeJson(Map.of("inputs", texts));
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EMBED))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkStatus(response, "Embedding server");
        return Json.parseEmbeddingsResponse(response.body());
    }

    public static void chat(Dialogue dialogue, Usage usage) throws IOException, InterruptedException {
        String json = Json.makeJson(dialogue);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AiProperties.getUrl()))
                .header("Authorization", "Bearer " + AiProperties.getKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response, "AI");
        String content = Json.getAiResponse(response.body());
        List<ToolCall> toolCalls = Json.parseToolCalls(response.body());
        usage.add(Json.getUsage(response.body()));
        if (!toolCalls.isEmpty()) {
            dialogue.addAssistantToolCalls(content, toolCalls);
        } else {
            dialogue.addAssistantMessage(content);
        }
    }

    private static void checkStatus(HttpResponse<String> response, String serviceName) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(serviceName + " returned HTTP " + response.statusCode() + ": " + response.body());
        }
    }
}