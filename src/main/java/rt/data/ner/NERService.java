package rt.data.ner;

import rt.common.entities_and_dtos.NamedEntity;

import java.util.List;
import java.util.Set;

public class NERService {
    private final EntityLoader entityLoader = new EntityLoader();
    private final EntityFinder entityFinder = new EntityFinder();
    private final List<NamedEntity> loadedEntities;

    public NERService() {
        loadedEntities = entityLoader.loadAndGet();
        entityFinder.setEntities(loadedEntities);
    }

    public Set<NamedEntity> extractEntities(String text) {
        return entityFinder.extractEntities(text);
    }
}
