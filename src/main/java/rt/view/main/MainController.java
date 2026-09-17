package rt.view.main;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import rt.core.Core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainController {

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
    private Label queueSizeLabel;

    @FXML
    VBox aiMessagesContainer;

    @FXML
    private TextArea aiInput;

    private Core core;
    private Map<Long, String> allChannels = new HashMap<>();
    private Map<Integer, String> allFolders = new LinkedHashMap<>();
    private final ObservableList<String> channelsSelectedItems = FXCollections.observableArrayList();
    private final ObservableList<String> foldersSelectedItems = FXCollections.observableArrayList();

    void setCore(Core core) {
        this.core = core;
    }

    void setChannels(Map<Long, String> channels) {
        if (channels == null) {
            allChannels = new HashMap<>();
        } else {
            allChannels = new HashMap<>(channels);
        }
        ObservableList<String> items = FXCollections.observableArrayList(allChannels.values());
        items.sort(String::compareToIgnoreCase);
        channelsSelectedItems.retainAll(items);
        channelsComboBox.getItems().setAll(items);
        Elements.instance().updateButtonCell(channelsComboBox, channelsSelectedItems, "Выберите каналы");
        Elements.instance().refreshComboBoxCells(channelsComboBox);
    }

    void setFolders(Map<Integer, String> folders) {
        if (folders == null) {
            allFolders = new LinkedHashMap<>();
        } else {
            allFolders = new LinkedHashMap<>(folders);
        }
        ObservableList<String> items = FXCollections.observableArrayList(allFolders.values());
        items.sort(String::compareToIgnoreCase);
        foldersSelectedItems.retainAll(items);
        foldersComboBox.getItems().setAll(items);
        Elements.instance().updateButtonCell(foldersComboBox, foldersSelectedItems, "Выберите папки");
        Elements.instance().refreshComboBoxCells(foldersComboBox);
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
        if (size <= 0) {
            queueSizeLabel.setText("Нет сообщений в обработке");
        } else {
            queueSizeLabel.setText("В обработке сообщений: " + size);
        }
    }

    @FXML
    private void initialize() {
        Elements.instance().setupMultiSelectComboBox(channelsComboBox, channelsSelectedItems, "Все каналы");
        Elements.instance().setupMultiSelectComboBox(foldersComboBox, foldersSelectedItems, "Все папки");
        Elements.instance().setupNotificationCopy(notificationListView);
    }

    @FXML
    private void onExportToCSV() {
        core.exportToCSV();
    }

    @FXML
    private void onSearch() {
        core.parseMessages(getSource(), dateFrom.getValue(), dateTo.getValue());
    }

    @FXML
    private void onSendAIMessage(ActionEvent event) {
        if (core.isThinking()) return;
        String message = aiInput.getText();
        if (message == null || message.isBlank()) return;
        message = message.trim();
        aiInput.clear();
        Elements.instance().addAIMessage(aiMessagesContainer, message, true);
        Elements.instance().showThinkingIndicator(aiMessagesContainer);
        core.askAgent(message);
    }

    public void showAgentsAnswer(String answer) {
        Platform.runLater(() -> {
            Elements.instance().hideThinkingIndicator(aiMessagesContainer);
            if (answer == null || answer.isBlank()) return;
            Elements.instance().addAIMessage(aiMessagesContainer, answer, false);
        });
    }

    @FXML
    private void onSetAIContext(ActionEvent actionEvent) {
        System.out.println("ctx");
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

    private Set<Long> getSource() {
        return Stream.concat(
                allFolders.entrySet().stream()
                        .filter(entry -> foldersSelectedItems.contains(entry.getValue()))
                        .map(entry -> entry.getKey().longValue()),
                allChannels.entrySet().stream()
                        .filter(entry -> channelsSelectedItems.contains(entry.getValue()))
                        .map(Map.Entry::getKey)
        ).collect(Collectors.toSet());
    }
}