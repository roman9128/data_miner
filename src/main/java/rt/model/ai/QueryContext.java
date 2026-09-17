package rt.model.ai;

import rt.model.ne.NamedEntity;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public record QueryContext(
        Set<Long> chatIds,
        LocalDate dateFrom,
        LocalDate dateTo,
        Set<NamedEntity> namedEntities,
        Map<String, Double> topicConfidenceMap
) {
}