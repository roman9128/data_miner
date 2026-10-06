package rt.core;

import rt.model.document.RawMessage;

public interface AssistantParser {
    void closeAuthWindow();
    void showQrCode(String link);
    void addRawMessage(RawMessage rawMessage);
}
