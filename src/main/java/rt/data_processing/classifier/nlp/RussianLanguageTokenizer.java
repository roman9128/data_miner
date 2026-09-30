package rt.data_processing.classifier.nlp;

import com.github.demidko.aot.WordformMeaning;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import rt.utils.TextUtils;

import java.util.Arrays;
import java.util.Set;

class RussianLanguageTokenizer {

    private final Set<String> stopWords = WordsLoader.loadWordsSet("ai/nlp/dictionaries/stop.txt");
    private final Set<String> toRemoveStrings = WordsLoader.loadWordsSet("ai/nlp/dictionaries/remove.txt");

    String[] tokenize(String text) {
        String[] words = TextUtils.getWordsAsLettersFrom(text);
        words = Arrays.stream(words)
                .filter(w -> w.length() > 1 && w.length() < 30)
                .filter(w -> !toRemove(w))
                .filter(w -> !isStopWord(w))
                .toArray(String[]::new);
        return lemmatizeAndStemmize(words);
    }

    private String[] lemmatizeAndStemmize(String[] wordsToChange) {
        for (int i = 0; i < wordsToChange.length; i++) {
            var meanings = WordformMeaning.lookupForMeanings(wordsToChange[i]);
            if (meanings.isEmpty()) {
                wordsToChange[i] = TextUtils.stem(wordsToChange[i]);
            } else {
                wordsToChange[i] = meanings.getFirst().getLemma().toString();
            }
        }
        return Arrays.stream(wordsToChange)
                .filter(w -> !isStopWord(w))
                .toArray(String[]::new);
    }

    private boolean toRemove(String word) {
        if (toRemoveStrings.size() == 1 && toRemoveStrings.contains("")) {
            return false;
        }
        return toRemoveStrings.stream().anyMatch(word::contains);
    }

    private boolean isStopWord(String word) {
        return stopWords.contains(word);
    }
}