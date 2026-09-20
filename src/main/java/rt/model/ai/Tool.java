package rt.model.ai;

import com.fasterxml.jackson.annotation.JsonValue;
import rt.utils.JsonUtils;

import java.util.Map;

public interface Tool {

    String getName();

    String getDescription();

    String getParameters();

    String execute(String arguments);

    void setQueryContext(QueryContext queryContext);

    @JsonValue
    default Map<String, Object> toJson() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", getDescription(),
                        "parameters", JsonUtils.readTree(getParameters())
                )
        );
    }
}