package rt.data.analyzer;

import rt.data.analyzer.ner.NERService;
import rt.data.analyzer.nlp.NLPService;
import rt.common.entities_and_dtos.NamedEntity;

import java.util.Map;
import java.util.Set;

public class Analyzer {

    private final NLPService nlpService = new NLPService();
    private final NERService nerService = new NERService();

    public Map<String, Double> classify(String text) {
        return nlpService.classify(text);
    }

    public Set<NamedEntity> recognizeNE(String text) {
        return nerService.extractEntitiesByPartialName(text);
    }
}