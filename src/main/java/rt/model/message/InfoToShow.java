package rt.model.message;

public record InfoToShow(
        String text,
        String link,
        String publishedAt,
        String chatName
) {
}
