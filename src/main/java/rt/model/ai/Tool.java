package rt.model.ai;

import com.fasterxml.jackson.annotation.JsonValue;
import rt.common_utils.Json;

import java.util.Map;

public interface Tool {

    String getName();

    String getDescription();

    String getParameters();

    String execute(String arguments);

    @JsonValue
    default Map<String, Object> toJson() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", getDescription(),
                        "parameters", Json.readTree(getParameters())
                )
        );
    }
}