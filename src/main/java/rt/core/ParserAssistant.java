package rt.core;

import rt.model.message.RawMessageRecord;

public interface ParserAssistant {
    void startInteractions();
    void showQrCode(String link);
    void addRawMessageRecord(RawMessageRecord rawMessageRecord);
}
