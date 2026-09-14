package rt.model.message;

import it.tdlight.jni.TdApi;

public record RawMessageRecord(
        TdApi.Message message,
        String chatName,
        String link
) {
}