package rt.data.noun_extractor;

import rt.common.ExternalAPIHandler;
import rt.common.Notifier;
import rt.common.entities_and_dtos.Notification;
import rt.common.entities_and_dtos.Noun;

import java.util.List;

public class NounExtractor {

    private final ExternalAPIHandler externalAPIHandler;

    public NounExtractor(ExternalAPIHandler externalAPIHandler) {
        this.externalAPIHandler = externalAPIHandler;
    }

    public List<Noun> extract(String text) {
        try {
            return externalAPIHandler.getNouns(text);
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.SHOW_USER, e.toString());
            return List.of();
        }
    }
}