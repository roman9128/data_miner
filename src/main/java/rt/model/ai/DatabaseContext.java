package rt.model.ai;

import rt.model.ne.NamedEntity;

import java.util.Map;
import java.util.Set;

public record DatabaseContext(
        Map<Long, String> chats,
        Set<NamedEntity> namedEntities,
        Set<String> topics
) {
}