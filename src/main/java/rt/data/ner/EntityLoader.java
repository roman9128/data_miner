package rt.data.ner;

import rt.model.ne.NamedEntity;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class EntityLoader {
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<NamedEntity> entities = new ArrayList<>();
    private final String PATH = "./ai/ner";

    List<NamedEntity> loadAndGet() {
        File folder = new File(PATH);
        if (!folder.exists() || !folder.isDirectory()) {
            return entities;
        }
        File[] jsonFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null) return entities;

        for (File file : jsonFiles) {
            List<NamedEntity> entitiesFromFile = mapper.readValue(file, new TypeReference<>() {
            });
            entities.addAll(entitiesFromFile);
        }
        return entities;
    }
}
