package rt.data.analyzer.ner;

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

    public Set<NamedEntity> extractEntitiesByPartialName(String text) {
        return entityFinder.extractEntitiesByPartialName(text);
    }

    public Set<NamedEntity> extractEntitiesByExactName(String text) {
        return entityFinder.extractEntitiesByExactName(text);
    }
}
