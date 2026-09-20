package rt.data_processing.noun_extractor;

import rt.api.ExternalAPIHandler;
import rt.core.Notifier;
import rt.model.notification.Notification;
import rt.model.noun.Noun;
import rt.utils.TextUtils;

import java.util.List;

public class NounExtractor {

    private final ExternalAPIHandler externalAPIHandler;

    public NounExtractor(ExternalAPIHandler externalAPIHandler) {
        this.externalAPIHandler = externalAPIHandler;
    }

    public List<Noun> extract(String text) {
        try {
            return externalAPIHandler.getNouns(TextUtils.normalize(text));
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Ошибка: " + e + "\nНевозможно извлечь существительные из текста: " + text);
            return List.of();
        }
    }
}