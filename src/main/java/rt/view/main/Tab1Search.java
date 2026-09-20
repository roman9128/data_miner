package rt.view.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Tab1Search {
    private static final Tab1Search INSTANCE = new Tab1Search();
    private Map<Long, String> allChannels = new HashMap<>();
    private Map<Integer, String> allFolders = new LinkedHashMap<>();
    private final ObservableList<String> channelsSelectedItems = FXCollections.observableArrayList();
    private final ObservableList<String> foldersSelectedItems = FXCollections.observableArrayList();

    private Tab1Search() {
    }

    static Tab1Search instance() {
        return INSTANCE;
    }

    Set<Long> getSource() {
        return Stream.concat(
                allFolders.entrySet().stream()
                        .filter(entry -> foldersSelectedItems.contains(entry.getValue()))
                        .map(entry -> entry.getKey().longValue()),
                allChannels.entrySet().stream()
                        .filter(entry -> channelsSelectedItems.contains(entry.getValue()))
                        .map(Map.Entry::getKey)
        ).collect(Collectors.toSet());
    }

    void setupNotificationCopy(ListView<String> notificationListView) {
        notificationListView.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.A) {
                notificationListView.getSelectionModel().selectAll();
                event.consume();
            }
            if (event.isControlDown() && event.getCode() == KeyCode.C) {
                copySelectedNotifications(notificationListView);
                event.consume();
            }
        });
    }

    void setChannels(Map<Long, String> channels, ComboBox<String> channelsComboBox) {
        if (channels == null) {
            allChannels = new HashMap<>();
        } else {
            allChannels = new HashMap<>(channels);
        }
        ObservableList<String> items = FXCollections.observableArrayList(allChannels.values());
        items.sort(String::compareToIgnoreCase);
        channelsSelectedItems.retainAll(items);
        channelsComboBox.getItems().setAll(items);
        updateButtonCell(channelsComboBox, channelsSelectedItems, "Выберите каналы");
        refreshComboBoxCells(channelsComboBox);
    }

    void setFolders(Map<Integer, String> folders, ComboBox<String> foldersComboBox) {
        if (folders == null) {
            allFolders = new LinkedHashMap<>();
        } else {
            allFolders = new LinkedHashMap<>(folders);
        }
        ObservableList<String> items = FXCollections.observableArrayList(allFolders.values());
        items.sort(String::compareToIgnoreCase);
        foldersSelectedItems.retainAll(items);
        foldersComboBox.getItems().setAll(items);
        updateButtonCell(foldersComboBox, foldersSelectedItems, "Выберите папки");
        refreshComboBoxCells(foldersComboBox);
    }

    void updateQueueSize(Controller controller, int size) {
        if (size <= 0) {
            controller.queueSizeLabel.setText("Нет сообщений в обработке");
        } else {
            controller.queueSizeLabel.setText("В обработке сообщений: " + size);
        }
    }

    private void copySelectedNotifications(ListView<String> notificationListView) {
        String text = notificationListView
                .getSelectionModel()
                .getSelectedItems()
                .stream()
                .collect(Collectors.joining(
                        System.lineSeparator()
                ));
        if (text.isEmpty()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    void setupMultiSelectComboBox(ComboBox<String> comboBox, String promptText) {
        ObservableList<String> selectedItems;
        if (promptText.equals(All.CHANNELS)) {
            selectedItems = channelsSelectedItems;
        } else if (promptText.equals(All.FOLDERS)) {
            selectedItems = foldersSelectedItems;
        } else return;

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
                                setText(null);
                                setGraphic(null);
                                return;
                            }
                            setText(null);
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
        } else {
            cell.setText("Выбрано: " + selectedItems.size());
        }
        cell.setGraphic(null);
    }

    private void refreshComboBoxCells(ComboBox<String> comboBox) {
        comboBox.getItems().setAll(new ArrayList<>(comboBox.getItems()));
    }
}