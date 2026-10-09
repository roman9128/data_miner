package rt.data_processing.noun_extractor;

import rt.api.ExternalAPI;
import rt.common_utils.Text;
import rt.model.notification.Notification;
import rt.model.noun.Noun;
import rt.notifier.Notifier;

import java.util.List;

public class NounExtractor {

    public List<Noun> extract(String text) {
        try {
            return ExternalAPI.getNouns(Text.normalize(text));
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Ошибка: " + e + "\nНевозможно извлечь существительные из текста: " + text);
            return List.of();
        }
    }
}