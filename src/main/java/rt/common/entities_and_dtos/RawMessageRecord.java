package rt.common.entities_and_dtos;

import it.tdlight.jni.TdApi;

public record RawMessageRecord(
        TdApi.Message message,
        String chatName,
        String link
) {
}