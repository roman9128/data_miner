package rt.data_processing.classifier;

import rt.data_processing.classifier.nlp.NLPClassifier;
import rt.data_processing.classifier.vector.VectorClassifier;
import rt.data_processing.embedder.EmbeddingClient;

import java.util.HashMap;
import java.util.Map;

public class Classifier {

    private final NLPClassifier nlpClassifier;
    private final VectorClassifier vectorClassifier;

    public Classifier(EmbeddingClient embeddingClient) {
        nlpClassifier = new NLPClassifier();
        vectorClassifier = new VectorClassifier(embeddingClient);
    }

    public Map<String, Double> classify(String text, float[] textEmb) {
        Map<String, Double> result = new HashMap<>();
        result.putAll(nlpClassifier.classify(text));
        result.putAll(vectorClassifier.classify(textEmb));
        return result;
    }
}
