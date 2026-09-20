package rt.data_processing.nlp;

import java.util.Map;

public class NLPService {

    private final NLPClassifier classifier = new NLPClassifier();
    private final RussianLanguageTokenizer tokenizer = new RussianLanguageTokenizer();

    public NLPService() {
        NLPModelFinder modelFinder = new NLPModelFinder();
        modelFinder.findModels();
        classifier.setModels(modelFinder.getModels());
    }

    public Map<String, Double> classify(String text) {
        String[] tokenizedText = tokenizer.tokenize(text);
        return classifier.classify(tokenizedText);
    }
}
