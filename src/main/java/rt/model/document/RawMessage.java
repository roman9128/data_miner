package rt.model.document;

import java.time.LocalDateTime;

public interface RawMessage {

    ContentSource contentSource();

    String sourceName();

    String sourceId();

    String sourceDocumentId();

    ContentType contentType();

    LocalDateTime dateTime();

    String link();

    String text();
}
