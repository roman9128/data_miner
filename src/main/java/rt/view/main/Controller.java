package rt.view.main;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import rt.core.Core;
import rt.model.ai.DatabaseContext;
import rt.model.ai.QueryContext;
import rt.model.ne.NamedEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller {

    @FXML
    private ComboBox<String> channelsComboBox;

    @FXML
    private ComboBox<String> foldersComboBox;

    @FXML
    private DatePicker dateFrom;

    @FXML
    private DatePicker dateTo;

    @FXML
    private ListView<String> notificationListView;

    @FXML
    Label queueSizeLabel;

    @FXML
    private VBox aiMessagesContainer;

    @FXML
    TextArea aiInput;

    @FXML
    private Button aiContextButton;

    @FXML
    ComboBox<String> contextChatsComboBox;

    @FXML
    DatePicker contextDateFrom;

    @FXML
    DatePicker contextDateTo;

    @FXML
    ComboBox<String> contextCategoryComboBox;

    @FXML
    ComboBox<String> contextTagComboBox;

    @FXML
    ListView<NamedEntity> contextEntitiesListView;

    @FXML
    ComboBox<String> contextTopicsComboBox;

    private Core core;

    void setCore(Core core) {
        this.core = core;
    }

    DatabaseContext getDatabaseContext() {
        return core.getDatabaseContext();
    }

    void setQueryContext(QueryContext queryContext) {
        core.setQueryContext(queryContext);
    }

    void setChannels(Map<Long, String> channels) {
        Tab1Search.instance().setChannels(channels, channelsComboBox);
    }

    void setFolders(Map<Integer, String> folders) {
        Tab1Search.instance().setFolders(folders, foldersComboBox);
    }

    public void addNotification(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        notificationListView.getItems().add("[" + dateTime + "]  " + message);
        notificationListView.scrollTo(notificationListView.getItems().size() - 1);
    }

    void updateQueueSize(int size) {
        Tab1Search.instance().updateQueueSize(this, size);
    }

    @FXML
    private void initialize() {
        Tab1Search.instance().setupMultiSelectComboBox(channelsComboBox, All.CHANNELS);
        Tab1Search.instance().setupMultiSelectComboBox(foldersComboBox, All.FOLDERS);
        Tab1Search.instance().setupNotificationCopy(notificationListView);
        Tab2Context.instance().initialize(this);
        Tab3AI.instance().setupAIInput(this);
    }

    void loadDatabaseContext() {
        Tab2Context.instance().loadDatabaseContext();
    }

    @FXML
    private void onExportToCSV() {
        core.exportToCSV();
    }

    @FXML
    private void onSearch() {
        core.parseMessages(Tab1Search.instance().getSource(), dateFrom.getValue(), dateTo.getValue());
    }

    @FXML
    void onSendAIMessage() {
        if (core.isThinking() || core.isBusy()) return;
        String message = aiInput.getText();
        if (message == null || message.isBlank()) return;
        message = message.trim();
        aiInput.clear();
        Tab3AI.instance().addAIMessage(aiMessagesContainer, message, true);
        Tab3AI.instance().showThinkingIndicator(aiMessagesContainer);
        core.askAgent(message);
    }

    void showAgentsAnswer(String answer) {
        Platform.runLater(() -> {
            Tab3AI.instance().hideThinkingIndicator(aiMessagesContainer);
            if (answer == null || answer.isBlank()) return;
            Tab3AI.instance().addAIMessage(aiMessagesContainer, answer, false);
        });
    }

    @FXML
    private void onClearChat() {
        core.clearChat();
        Tab3AI.instance().clearChat(aiMessagesContainer);
    }

    @FXML
    private void onToday() {
        LocalDate today = LocalDate.now();
        dateFrom.setValue(today);
        dateTo.setValue(today);
    }

    @FXML
    private void onYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        dateFrom.setValue(yesterday);
        dateTo.setValue(yesterday);
    }

    @FXML
    private void onLast7Days() {
        LocalDate today = LocalDate.now();
        dateFrom.setValue(today.minusDays(6));
        dateTo.setValue(today);
    }

    @FXML
    private void onLast14Days() {
        LocalDate today = LocalDate.now();
        dateFrom.setValue(today.minusDays(13));
        dateTo.setValue(today);
    }

    @FXML
    private void onLast30Days() {
        LocalDate today = LocalDate.now();
        dateFrom.setValue(today.minusDays(29));
        dateTo.setValue(today);
    }

    @FXML
    private void onContextToday() {
        LocalDate today = LocalDate.now();
        contextDateFrom.setValue(today);
        contextDateTo.setValue(today);
    }

    @FXML
    private void onContextYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        contextDateFrom.setValue(yesterday);
        contextDateTo.setValue(yesterday);
    }

    @FXML
    private void onContextLast7Days() {
        LocalDate today = LocalDate.now();
        contextDateFrom.setValue(today.minusDays(6));
        contextDateTo.setValue(today);
    }

    @FXML
    private void onContextLast14Days() {
        LocalDate today = LocalDate.now();
        contextDateFrom.setValue(today.minusDays(13));
        contextDateTo.setValue(today);
    }

    @FXML
    private void onContextLast30Days() {
        LocalDate today = LocalDate.now();
        contextDateFrom.setValue(today.minusDays(29));
        contextDateTo.setValue(today);
    }

    @FXML
    private void onSelectAllContextEntities() {
        Tab2Context.instance().selectAllFilteredEntities();
    }

    @FXML
    private void onClearContextEntities() {
        Tab2Context.instance().clearFilteredEntities();
    }

    @FXML
    private void onToggleAIContext() {
        Tab2Context context = Tab2Context.instance();
        if (!context.hasConfiguredContext()) {
            aiContextButton.getStyleClass().remove("context-active");
            aiContextButton.setText("Контекст ИИ: не задан");
            return;
        }
        if (aiContextButton.getStyleClass().contains("context-active")) {
            core.setQueryContext(null);
            aiContextButton.getStyleClass().remove("context-active");
            aiContextButton.setText("Контекст ИИ: выключен");
            return;
        }
        context.applyContext();
        if (!aiContextButton.getStyleClass().contains("context-active")) {
            aiContextButton.getStyleClass().add("context-active");
        }
        aiContextButton.setText("Контекст ИИ: используется");
    }

    @FXML
    private void onClearAIContext() {
        Tab2Context.instance().clearContext();
        aiContextButton.getStyleClass().remove("context-active");
        aiContextButton.setText("Контекст ИИ: не задан");
    }
}