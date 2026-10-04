package rt.model.document;

import it.tdlight.jni.TdApi;

public record RawMessage(
        TdApi.Message message,
        ContentSource source,
        String chatName,
        String link
) {
}