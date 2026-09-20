package rt.data_processing.ner;

import rt.model.ne.NamedEntity;

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
