package rt.data_processing.classifier;

import rt.data_processing.classifier.nlp.NLPClassifier;
import rt.data_processing.classifier.vector.VectorClassifier;
import rt.data_processing.embedder.EmbeddingClient;

import java.util.HashSet;
import java.util.Set;

public class Classifier {

    private final NLPClassifier nlpClassifier;
    private final VectorClassifier vectorClassifier;

    public Classifier(EmbeddingClient embeddingClient) {
        nlpClassifier = new NLPClassifier();
        vectorClassifier = new VectorClassifier(embeddingClient);
    }

    public Set<String> classify(String text, float[] textEmb) {
        Set<String> result = new HashSet<>();
        result.addAll(nlpClassifier.classify(text).keySet());
        result.addAll(vectorClassifier.classify(textEmb).keySet());
        return result;
    }
}
