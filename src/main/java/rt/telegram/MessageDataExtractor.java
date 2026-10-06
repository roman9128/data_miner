package rt.telegram;

import it.tdlight.jni.TdApi;
import rt.common_utils.Text;
import rt.model.document.ContentType;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class MessageDataExtractor {

    private MessageDataExtractor() {
    }

    public static TelegramMessageTypeText extractTypeAndTextFromTelegramMessage(TdApi.Message message) {
        TdApi.MessageContent messageContent = message.content;
        switch (messageContent) {
            case TdApi.MessageText text -> {
                return new TelegramMessageTypeText(
                        ContentType.TEXT,
                        Text.getRidOfNull(text.text.text));
            }

            case TdApi.MessagePhoto photo -> {
                return new TelegramMessageTypeText(
                        ContentType.PHOTO,
                        Text.getRidOfNull(photo.caption.text));
            }
            case TdApi.MessageVideo video -> {
                return new TelegramMessageTypeText(
                        ContentType.VIDEO,
                        Text.getRidOfNull(video.caption.text));
            }
            case TdApi.MessageDocument document -> {
                return new TelegramMessageTypeText(
                        ContentType.DOCUMENT,
                        Text.getRidOfNull(document.caption.text));
            }
            case TdApi.MessageAudio audio -> {
                return new TelegramMessageTypeText(
                        ContentType.AUDIO,
                        Text.getRidOfNull(audio.caption.text));
            }
            case TdApi.MessagePoll poll -> {
                return new TelegramMessageTypeText(
                        ContentType.POLL,
                        getPollTexts(poll));
            }
            case TdApi.MessageAnimation animation -> {
                return new TelegramMessageTypeText(
                        ContentType.ANIMATION,
                        Text.getRidOfNull(animation.caption.text));
            }
            case TdApi.MessageVideoNote videoNote -> {
                return new TelegramMessageTypeText(
                        ContentType.VIDEO_NOTE,
                        "");
            }
            case TdApi.MessageVoiceNote voiceNote -> {
                return new TelegramMessageTypeText(
                        ContentType.VOICE_NOTE,
                        Text.getRidOfNull(voiceNote.caption.text));
            }
            default -> {
                return new TelegramMessageTypeText(
                        ContentType.MISC,
                        Text.getRidOfNull(messageContent.toString())
                );
            }
        }
    }

    private static String getPollTexts(TdApi.MessagePoll poll) {
        String question = Text.getRidOfNull(poll.poll.question);
        String options = Arrays.stream(poll.poll.options)
                .map(o -> Text.getRidOfNull(o.text))
                .filter(o -> !o.isBlank())
                .map(o -> "\n- " + o)
                .collect(Collectors.joining());
        return question + options;
    }

    public record TelegramMessageTypeText(ContentType contentType, String text) {
    }
}
