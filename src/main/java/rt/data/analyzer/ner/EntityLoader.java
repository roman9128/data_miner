package rt.data.analyzer.ner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import rt.common.entities_and_dtos.NamedEntity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class EntityLoader {
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<NamedEntity> entities = new ArrayList<>();
    private final String PATH = "./ne_json";

    List<NamedEntity> loadAndGet() {
        File folder = new File(PATH);
        if (!folder.exists() || !folder.isDirectory()) {
            return entities;
        }
        File[] jsonFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null) return entities;

        for (File file : jsonFiles) {
            try {
                List<NamedEntity> entitiesFromFile = mapper.readValue(file, new TypeReference<>() {
                });
                entities.addAll(entitiesFromFile);
            } catch (IOException e) {
                // ignore
            }
        }
        return entities;
    }
}
