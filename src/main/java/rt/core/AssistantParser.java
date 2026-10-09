package rt.core;

import rt.model.document.RawMessage;

public interface AssistantParser {
    void closeTelegramAuthWindow();
    void showTelegramQrCode(String link);
    void addRawMessage(RawMessage rawMessage);
}
