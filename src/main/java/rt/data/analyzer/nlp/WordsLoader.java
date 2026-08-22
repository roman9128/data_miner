package rt.data.analyzer.nlp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class WordsLoader {

    static Set<String> loadWordsSet(String fileName) {
        try {
            String[] stopWordsArr = readTextFromFile(fileName).split("\\s+");
            return new HashSet<>(List.of(stopWordsArr));
        } catch (IOException e) {
            return new HashSet<>();
        }
    }

    static String readTextFromFile(String fileName) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }
            return builder.toString();
        }
    }
}
