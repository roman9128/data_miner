package rt.common.entities_and_dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Set;

public class NamedEntity {
    private final String name;
    private final Set<String> synonyms;
    private final String category;
    private final String description;
    private final Set<String> tags;

    @JsonCreator
    public NamedEntity(
            @JsonProperty("name") String name,
            @JsonProperty("synonyms") Set<String> synonyms,
            @JsonProperty("category") String category,
            @JsonProperty("description") String description,
            @JsonProperty("tags") Set<String> tags
    ) {
        this.name = name;
        this.synonyms = synonyms;
        this.category = category;
        this.description = description;
        this.tags = tags;
    }

    public String getName() {
        return name;
    }

    public Set<String> getSynonyms() {
        return synonyms;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NamedEntity namedEntity)) return false;
        return Objects.equals(getName(), namedEntity.getName()) &&
                Objects.equals(getCategory(), namedEntity.getCategory());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getCategory());
    }
}