package rt.data_processing.classifier.nlp;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import rt.core.Notifier;
import rt.model.notification.Notification;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class NLPClassifier {

    private final Map<String, DocumentCategorizerME> models = new HashMap<>();
    private final RussianLanguageTokenizer tokenizer = new RussianLanguageTokenizer();

    public NLPClassifier() {
        this.models.putAll(getModels());
    }

    private Map<String, DocumentCategorizerME> getModels() {

        String modelsDirString = "ai/nlp/models";
        String modelExtension = "model";
        Path modelsDir = Paths.get(modelsDirString);

        if (!Files.exists(modelsDir) || !Files.isDirectory(modelsDir)) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Указанный путь не является директорией или не существует");
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modelsDir, "*.{" + modelExtension + "}")) {
            for (Path path : stream) {
                File modelFile = path.toFile();
                String fileName = modelFile.getName();
                String label = fileName.substring(0, fileName.length() - modelExtension.length() - 1); // удаляю расширение .model
                DoccatModel model = new DoccatModel(modelFile);
                models.put(label, new DocumentCategorizerME(model));
            }
        } catch (IOException e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, e.toString());
        }
        return models;
    }

    /**
     * Метод оценивает вероятность соответствия текста определённой категории от 0 до 100%.
     * Вероятность менее 55% не учитывается
     *
     * @param text текст для анализа
     * @return HashMap категория-вероятность
     */
    public Map<String, Double> classify(String text) {
        String[] tokens = tokenizer.tokenize(text);

        Map<String, Double> result = new HashMap<>();

        for (Map.Entry<String, DocumentCategorizerME> entry : models.entrySet()) {
            String label = entry.getKey();
            DocumentCategorizerME categorizer = entry.getValue();
            double[] probabilities = categorizer.categorize(tokens);
            int labelIndex = categorizer.getIndex(label);
            double labelProbability = probabilities[labelIndex];
            if (labelProbability >= 0.6) {
                result.put(label, labelProbability * 100);
            }
        }
        return result;
    }
}