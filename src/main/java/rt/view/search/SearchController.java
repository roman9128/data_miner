package rt.view.search;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import rt.core.Core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchController {

    @FXML
    private ComboBox<String> channelsComboBox;

    @FXML
    private ComboBox<String> foldersComboBox;

    @FXML
    private DatePicker dateFrom;

    @FXML
    private DatePicker dateTo;

    private Stage stage;
    private Core core;
    private Map<Long, String> allChannels = new HashMap<>();
    private Map<Integer, String> allFolders = new HashMap<>();
    private final ObservableList<String> channelsSelectedItems = FXCollections.observableArrayList();
    private final ObservableList<String> foldersSelectedItems = FXCollections.observableArrayList();
    @FXML
    private ListView<String> notificationListView;
    @FXML
    private Label queueSizeLabel;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setCore(Core core) {
        this.core = core;
    }

    public void setChannels(Map<Long, String> channels) {
        if (channels == null) {
            allChannels = new HashMap<>();
        } else {
            allChannels = new HashMap<>(channels);
        }
        ObservableList<String> items = FXCollections.observableArrayList(allChannels.values());
        items.sort(String::compareToIgnoreCase);
        channelsSelectedItems.retainAll(items);
        channelsComboBox.getItems().setAll(items);
        updateButtonCell(
                channelsComboBox,
                channelsSelectedItems,
                "Выберите каналы"
        );
        refreshComboBoxCells(channelsComboBox);
    }

    public void setFolders(Map<Integer, String> folders) {
        if (folders == null) {
            allFolders = new LinkedHashMap<>();
        } else {
            allFolders = new LinkedHashMap<>(folders);
        }
        ObservableList<String> items = FXCollections.observableArrayList(allFolders.values());
        items.sort(String::compareToIgnoreCase);
        foldersSelectedItems.retainAll(items);
        foldersComboBox.getItems().setAll(items);
        updateButtonCell(
                foldersComboBox,
                foldersSelectedItems,
                "Выберите папки"
        );
        refreshComboBoxCells(foldersComboBox);
    }

    public void addNotification(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        notificationListView.getItems().add("[" + dateTime + "]  " + message);
        notificationListView.scrollTo(notificationListView.getItems().size() - 1);
    }

    public void updateQueueSize(int size) {
        if (size <= 0) {
            queueSizeLabel.setText("Нет сообщений в обработке");
        } else {
            queueSizeLabel.setText("В обработке сообщений: " + size);
        }
    }

    private void setupMultiSelectComboBox(ComboBox<String> comboBox, ObservableList<String> selectedItems, String promptText) {
        comboBox.setPromptText(promptText);
        comboBox.setCellFactory(
                listView -> {
                    ListCell<String> cell = new ListCell<>() {
                        private final CheckBox checkBox = new CheckBox();

                        {
                            checkBox.setMouseTransparent(true);
                            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        }

                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setGraphic(null);
                                return;
                            }
                            checkBox.setText(item);
                            checkBox.setSelected(selectedItems.contains(item));
                            setGraphic(checkBox);
                            setOnMousePressed(event -> {
                                if (event.isPrimaryButtonDown()) {
                                    if (selectedItems.contains(item)) {
                                        selectedItems.remove(item);
                                    } else {
                                        selectedItems.add(item);
                                    }
                                    checkBox.setSelected(selectedItems.contains(item));
                                    event.consume();
                                    comboBox.show();
                                    updateButtonCell(comboBox, selectedItems, promptText);
                                }
                            });
                        }
                    };
                    return cell;
                }
        );

        comboBox.setButtonCell(
                new ListCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        updateButtonCell(this, selectedItems, promptText);
                    }
                }
        );

        comboBox.showingProperty().addListener(
                (observable, oldValue, showing) -> {
                    if (showing) {
                        refreshComboBoxCells(comboBox);
                    }
                }
        );
    }

    private void updateButtonCell(ComboBox<String> comboBox, ObservableList<String> selectedItems, String promptText) {
        ListCell<String> buttonCell = comboBox.getButtonCell();
        if (buttonCell == null) {
            return;
        }
        updateButtonCell(buttonCell, selectedItems, promptText);
    }

    private void updateButtonCell(ListCell<String> cell, ObservableList<String> selectedItems, String promptText) {
        if (selectedItems.isEmpty()) {
            cell.setText(promptText);
        } else if (selectedItems.size() <= 3) {
            cell.setText(String.join(", ", selectedItems));
        } else {
            cell.setText(
                    selectedItems.get(0)
                            + ", "
                            + selectedItems.get(1)
                            + ", "
                            + selectedItems.get(2)
                            + "..."
                            + " ("
                            + selectedItems.size()
                            + ")"
            );
        }

        cell.setGraphic(null);
    }

    private void refreshComboBoxCells(ComboBox<String> comboBox) {
        comboBox.getItems().setAll(new ArrayList<>(comboBox.getItems()));
    }

    @FXML
    private void initialize() {
        setupMultiSelectComboBox(channelsComboBox, channelsSelectedItems, "Выберите каналы");
        setupMultiSelectComboBox(foldersComboBox, foldersSelectedItems, "Выберите папки");
    }

    @FXML
    private void onExportToCSV() {
        core.exportToCSV();
    }

    @FXML
    private void onSearch() {
        if (core == null) return;
        core.parseMessages(getSource(), dateFrom.getValue(), dateTo.getValue());
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