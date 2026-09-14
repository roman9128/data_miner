package rt.utils;

import rt.model.ai.ToolCall;
import rt.model.noun.Noun;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static JsonNode readTree(String json) {
        return MAPPER.readTree(json);
    }

    public static List<ToolCall> parseToolCalls(String json) {
        JsonNode root = MAPPER.readTree(json);
        JsonNode calls = root
                .path("choices")
                .path(0)
                .path("message")
                .path("tool_calls");
        List<ToolCall> result = new ArrayList<>();
        if (!calls.isArray()) {
            return result;
        }
        for (JsonNode call : calls) {
            String id = call.path("id").asString();
            String name = call.path("function").path("name").asString();
            String arguments = call.path("function").path("arguments").asString();
            result.add(new ToolCall(id, name, arguments));
        }
        return result;
    }

    public static String makeJson(Object value) {
        return MAPPER.writeValueAsString(value);
    }

    public static String getAiResponse(String jsonResponse) {
        JsonNode root = MAPPER.readTree(jsonResponse);
        JsonNode content = root
                .path("choices")
                .path(0)
                .path("message")
                .path("content");
        return content.isNull() ? null : content.asString();
    }

    public static List<Noun> parseNounsResponse(String json) {
        JsonNode root = MAPPER.readTree(json);
        List<Noun> result = new ArrayList<>();
        JsonNode nouns = root.get("nouns");
        if (nouns != null && nouns.isArray()) {
            for (JsonNode noun : nouns) {
                String lemma = noun.path("lemma").asString();
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

    public static List<String> parseLemmasResponse(String json) {
        JsonNode root = MAPPER.readTree(json);
        List<String> result = new ArrayList<>();
        JsonNode lemmas = root.get("lemmas");
        if (lemmas != null && lemmas.isArray()) {
            for (JsonNode lemma : lemmas) {
                result.add(lemma.asString());
            }
        }
        return result;
    }

    public static float[][] parseEmbeddingsResponse(String json) {
        return MAPPER.readValue(json, float[][].class);
    }
}