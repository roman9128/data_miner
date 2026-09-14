package rt.data.ner;

import opennlp.tools.stemmer.snowball.SnowballStemmer;
import rt.model.ne.NamedEntity;
import rt.utils.TextUtils;

import java.util.*;

class EntityFinder {
    private final SnowballStemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.RUSSIAN);
    private static final int MAX_DISTANCE = 2;
    private Map<String, Map<Set<String>, NamedEntity>> index = Map.of();

    void setEntities(List<NamedEntity> entities) {
        Map<String, Map<Set<String>, NamedEntity>> newIndex = new HashMap<>();
        for (NamedEntity entity : entities) {
            addName(newIndex, entity, entity.getName());
            if (entity.getSynonyms() != null) {
                for (String synonym : entity.getSynonyms()) {
                    addName(newIndex, entity, synonym);
                }
            }
        }
        this.index = newIndex;
    }

    private void addName(Map<String, Map<Set<String>, NamedEntity>> index, NamedEntity entity, String name) {
        if (name == null || name.isBlank()) {
            return;
        }

        Set<String> textParts = new HashSet<>(stem(name));
        if (textParts.isEmpty()) {
            return;
        }
        for (String textPart : textParts) {
            index.computeIfAbsent(textPart, k -> new HashMap<>()).put(textParts, entity);
        }
    }

    Set<NamedEntity> extractEntities(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        List<String> textParts = stem(text);
        if (textParts.isEmpty()) {
            return Collections.emptySet();
        }
        Set<NamedEntity> result = new HashSet<>();
        int searchFromIndex = 0;

        for (int i = 0; i < textParts.size(); i++) {
            String textPart = textParts.get(i);
            Map<Set<String>, NamedEntity> candidates = index.get(textPart);
            if (candidates == null || candidates.isEmpty()) {
                continue;
            }
            List<Set<String>> candidateStems = candidates.keySet()
                    .stream()
                    .sorted(Comparator.comparing(Set<String>::size).reversed())
                    .toList();
            for (Set<String> candidateStem : candidateStems) {
                if (containsNearby(textParts, i, searchFromIndex, candidateStem)) {
                    result.add(candidates.get(candidateStem));
                    searchFromIndex = i + 1;
                    break;
                }
            }
        }
        return result;
    }

    private boolean containsNearby(List<String> textParts, int position, int searchFromIndex, Set<String> candidateParts) {
        int from = Math.max(Math.max(0, position - MAX_DISTANCE), searchFromIndex);
        int to = Math.min(textParts.size(), position + MAX_DISTANCE + 1);
        Set<String> found = new HashSet<>();
        for (int i = from; i < to; i++) {
            String textPart = textParts.get(i);
            if (candidateParts.contains(textPart)) {
                found.add(textPart);
            }
        }
        return found.containsAll(candidateParts);
    }

    private List<String> stem(String text) {
        if (text == null || text.isBlank()) return List.of();

        String[] words = TextUtils.getWordsFrom(text);
        if (words.length == 0) return List.of();

        List<String> result = new ArrayList<>();

        for (String word : words) {
            if (!word.isBlank()) result.add(stemmer.stem(word).toString());
        }
        return result;
    }
}