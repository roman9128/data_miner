package rt.telegram;

import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import rt.common.Notifier;
import rt.common.ParserAssistant;
import rt.common.config.CredentialsHandler;
import rt.common.config.AppPropertiesHandler;
import rt.common.entities_and_dtos.Notification;
import rt.common.entities_and_dtos.RawMessageRecord;
import rt.common.utils.NumberUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public final class TgClientWrapper implements AutoCloseable {

    private final SimpleTelegramClient client;
    private final ChatHistoryLoader chatHistoryLoader = new ChatHistoryLoader();
    private final ConcurrentMap<Long, TdApi.Chat> chats = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, String> foldersInfo = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, long[]> chatsInFolders = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, TdApi.Supergroup> supergroups = new ConcurrentHashMap<>();
    private final ParserAssistant assistant;
    private final AuthErrorHandler authErrorHandler;

    public TgClientWrapper(SimpleTelegramClientFactory clientFactory, ParserAssistant assistant) {

        this.assistant = assistant;
        this.authErrorHandler = new AuthErrorHandler();

        APIToken apiToken = new APIToken(CredentialsHandler.getApiID(), CredentialsHandler.getApiHash());
        TDLibSettings settings = TDLibSettings.create(apiToken);
        Path sessionPath = Paths.get("session");
        settings.setDatabaseDirectoryPath(sessionPath.resolve("data"));
        settings.setApplicationVersion("1.0.0");
        SimpleTelegramClientBuilder clientBuilder = clientFactory.builder(settings);
        SimpleTelegramClient client = clientBuilder.build(AuthenticationSupplier.qrCode());
        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, this::onUpdateAuthorizationState);
        client.addUpdateHandler(TdApi.UpdateNewChat.class, this::onUpdateChat);
        client.addUpdateHandler(TdApi.UpdateSupergroup.class, this::onUpdateSuperGroup);
        client.addUpdateHandler(TdApi.UpdateChatFolders.class, this::onUpdateFolder);
        client.setClientInteraction(null);
        this.client = client;
    }

    private void onUpdateAuthorizationState(TdApi.UpdateAuthorizationState update) {
        TdApi.AuthorizationState authorizationState = update.authorizationState;
        switch (authorizationState) {
            case TdApi.AuthorizationStateWaitOtherDeviceConfirmation deviceConfirmation -> {
                String link = deviceConfirmation.link;
                assistant.showQrCode(link);
            }
            case TdApi.AuthorizationStateWaitPassword waitPassword -> {
                client.send(new TdApi.CheckAuthenticationPassword(CredentialsHandler.getPassword()), authErrorHandler);
            }
            case TdApi.AuthorizationStateReady ready -> {
                assistant.startInteractions();
            }
            case TdApi.AuthorizationStateLoggingOut loggingOut -> {
                Notifier.instance().add(Notification.Level.SHOW_USER, "Разлогинен");
            }
            default -> {
            }
        }
    }

    private void onUpdateChat(TdApi.UpdateNewChat updateNewChat) {
        TdApi.Chat chat = updateNewChat.chat;
        chats.put(chat.id, chat);
    }

    private void onUpdateSuperGroup(TdApi.UpdateSupergroup updateSupergroup) {
        TdApi.Supergroup supergroup = updateSupergroup.supergroup;
        if (supergroup.status.getConstructor() == TdApi.ChatMemberStatusMember.CONSTRUCTOR && supergroup.isChannel) {
            supergroups.put(transferChatID(supergroup.id), supergroup);
        }
    }

    private void onUpdateFolder(TdApi.UpdateChatFolders updateChatFolders) {
        for (TdApi.ChatFolderInfo folder : updateChatFolders.chatFolders) {
            foldersInfo.put(folder.id, folder.title);
            getChatsFromFolder(folder.id);
        }
    }

    private void getChatsFromFolder(int folderID) {
        client.send(new TdApi.GetChatFolder(folderID)).whenComplete((folder, error) -> {
            if (error != null) {
                Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Ошибка при получении содержимого папок: " + error.getMessage());
            } else {
                chatsInFolders.put(folderID, folder.includedChatIds);
            }
        });
    }

    public Map<Integer, String> getFoldersIDsAndNames() {
        return new LinkedHashMap<>(foldersInfo);
    }

    public Set<Long> getChatsInFolder(int folderID) {
        if (!foldersInfo.containsKey(folderID)) {
            return new TreeSet<>();
        } else {
            return Arrays.stream(chatsInFolders.get(folderID))
                    .filter(this::isSupergroupInChats)
                    .boxed()
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    public Map<Long, String> getChannelsIDsAndNames() {
        return supergroups.keySet().stream().collect(Collectors.toMap(
                key -> key,
                key -> chats.get(key).title));
    }

    public void loadChannelsHistory(long channelID, long dateFromUnix, long dateToUnix) {
        long dateNowUnix = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();

        if (dateFromUnix >= dateToUnix) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Вторая дата не может быть больше первой");
            return;
        }
        if (dateFromUnix > dateNowUnix) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Этот день ещё не наступил");
            return;
        }

        chatHistoryLoader.setDateFromUnix(dateFromUnix);
        chatHistoryLoader.setDateToUnix(dateToUnix);

        if (isSupergroupInChats(channelID)) {
            loadChatHistory(channelID, dateFromUnix);
        }

        chatHistoryLoader.zeroCount();
        chatHistoryLoader.removeSurplus();
        prepareRecords();
    }

    private void loadChatHistory(long channelID, long dateFromUnix) {
        String title = chats.get(channelID).title;
        Notifier.instance().add(Notification.Level.SHOW_USER, "Загружаю сообщения из " + title);
        int messagesLeft = AppPropertiesHandler.getMessagesToDownload();
        int messagesToStop = AppPropertiesHandler.getMessagesToStop();
        long fromMessageID = 0;
        while (messagesToStop > 0) {
            try {
                TdApi.Messages messages = client.send(new TdApi.GetChatHistory(
                                channelID,
                                fromMessageID,
                                0,
                                75,
                                false))
                        .join();
                if (messages == null || messages.messages.length == 0) {
                    break;
                }
                chatHistoryLoader.acceptMessages(messages);
                fromMessageID = messages.messages[messages.messages.length - 1].id;
                messagesLeft -= messages.messages.length;
                messagesToStop -= messages.messages.length;

                if (dateFromUnix != 0L) {
                    if (messages.messages[messages.messages.length - 1].date <= dateFromUnix) {
                        break;
                    }
                } else {
                    if (messagesLeft < 1) {
                        break;
                    }
                }
                Thread.sleep(NumberUtils.giveRandomNumber());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Загрузка сообщений прервана: " + e);
                break;
            } catch (Exception e) {
                Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Ошибка загрузки сообщений из " + title + ": " + e);
                break;
            }
        }
        Notifier.instance().add(Notification.Level.SHOW_USER, "Загрузка сообщений из " + title + " закончена");
    }

    private long transferChatID(long chatID) {
        return -(1000000000000L + chatID);
    }

    private boolean isSupergroupInChats(long chatID) {
        return supergroups.containsKey(chatID) && supergroups.get(chatID).status.getConstructor() == TdApi.ChatMemberStatusMember.CONSTRUCTOR;
    }

    @Override
    public void close() {
        client.sendClose();
    }

    public void waitForExit() throws InterruptedException {
        client.waitForExit();
    }

    private void prepareRecords() {
        while (!chatHistoryLoader.isEmpty()) {
            TdApi.Message message = chatHistoryLoader.takeMessage();
            String senderName = chats.get(message.chatId).title;
            String link = getMsgLink(message);
            assistant.addRawMessageRecord(new RawMessageRecord(message, senderName, link));
        }
    }

    private String getMsgLink(TdApi.Message message) {
        try {
            return client.send(new TdApi.GetMessageLink(
                            message.chatId,
                            message.id,
                            0,
                            true,
                            true))
                    .get().link;
        } catch (Exception e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, e.getMessage());
            return "Не удалось получить ссылку";
        }
    }
}