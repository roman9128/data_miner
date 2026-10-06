package rt.data_processing.noun_extractor;

import rt.api.ExternalAPIHandler;
import rt.notifier.Notifier;
import rt.model.notification.Notification;
import rt.model.noun.Noun;
import rt.common_utils.Text;

import java.util.List;

public class NounExtractor {

    private final ExternalAPIHandler externalAPIHandler;

    public NounExtractor(ExternalAPIHandler externalAPIHandler) {
        this.externalAPIHandler = externalAPIHandler;
    }

    public List<Noun> extract(String text) {
        try {
            return externalAPIHandler.getNouns(Text.normalize(text));
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Ошибка: " + e + "\nНевозможно извлечь существительные из текста: " + text);
            return List.of();
        }
    }
}