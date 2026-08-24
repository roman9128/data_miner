package rt.data.analyzer.ner;

import rt.common.entities_and_dtos.NamedEntity;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class EntityFinder {

    private List<NamedEntity> entities;
    private Map<String, NamedEntity> nameMap;

    void setEntities(List<NamedEntity> entities) {
        this.entities = entities;
        this.nameMap = new HashMap<>();
        buildNameMap();
    }

    private void buildNameMap() {
        for (NamedEntity e : entities) {
            nameMap.put(e.getName().toLowerCase(), e);
            for (String syn : e.getSynonyms()) {
                nameMap.put(syn.toLowerCase(), e);
            }
        }
    }

    Set<NamedEntity> extractEntitiesByPartialName(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        String lowerText = text.toLowerCase();
        Set<NamedEntity> resultSet = new HashSet<>();

        for (NamedEntity e : entities) {
            if (e.getName() != null && lowerText.contains(e.getName().toLowerCase())) {
                resultSet.add(e);
                continue;
            }
            for (String syn : e.getSynonyms()) {
                if (syn != null && lowerText.contains(syn.toLowerCase())) {
                    resultSet.add(e);
                    break;
                }
            }
        }
        return resultSet;
    }

    Set<NamedEntity> extractEntitiesByExactName(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        String lowerText = text.toLowerCase();
        Set<NamedEntity> resultSet = new HashSet<>();
        for (NamedEntity e : entities) {
            if (matchesExactWord(lowerText, e.getName())) {
                resultSet.add(e);
                continue;
            }
            for (String syn : e.getSynonyms()) {
                if (matchesExactWord(lowerText, syn)) {
                    resultSet.add(e);
                    break;
                }
            }
        }
        return resultSet;
    }

    private boolean matchesExactWord(String text, String word) {
        if (word == null || word.isBlank()) return false;
        String pattern = "\\b" + Pattern.quote(word.toLowerCase()) + "\\b";
        return Pattern.compile(pattern).matcher(text).find();
    }
}