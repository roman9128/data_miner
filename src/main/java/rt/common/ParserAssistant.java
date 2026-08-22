package rt.common;

import rt.common.entities_and_dtos.RawMessageRecord;

public interface ParserAssistant {
    void startInteractions();
    void showQrCode(String link);
    void addRawMessageRecord(RawMessageRecord rawMessageRecord);
}
