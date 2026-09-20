package rt.view.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import rt.model.ai.DatabaseContext;
import rt.model.ai.QueryContext;
import rt.model.ne.NamedEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Tab2Context {

    private static final Tab2Context INSTANCE = new Tab2Context();

    private Controller controller;
    private DatabaseContext databaseContext;
    private QueryContext queryContext;
    private final ObservableList<String> selectedChats = FXCollections.observableArrayList();
    private final ObservableList<String> selectedTopics = FXCollections.observableArrayList();
    private final ObservableList<NamedEntity> selectedEntities = FXCollections.observableArrayList();
    private String selectedCategory;
    private String selectedTag;

    private Tab2Context() {
    }

    static Tab2Context instance() {
        return INSTANCE;
    }

    void initialize(Controller controller) {
        this.controller = controller;
        setupListeners();
        refreshUI();
    }

    void loadDatabaseContext() {
        DatabaseContext context = controller.getDatabaseContext();
        if (context == null) {
            databaseContext = null;
            clearAvailableData();
            return;
        }
        databaseContext = context;
        refreshChats();
        refreshEntityFilters();
        refreshTopics();
        refreshEntityList();
    }

    boolean hasConfiguredContext() {
        return queryContext != null;
    }

    private void updateQueryContext() {
        queryContext = new QueryContext(
                getSelectedChatIds(),
                getDateFrom(),
                getDateTo(),
                new HashSet<>(selectedEntities),
                new HashSet<>(selectedTopics)
        );
    }

    public void applyContext() {
        controller.setQueryContext(queryContext);
    }

    public void clearContext() {
        queryContext = null;
        selectedChats.clear();
        selectedTopics.clear();
        selectedEntities.clear();
        selectedCategory = null;
        selectedTag = null;
        if (controller.contextChatsComboBox != null) {
            refreshMultiSelectButton(controller.contextChatsComboBox, selectedChats, "Все каналы");
        }
        if (controller.contextTopicsComboBox != null) {
            refreshMultiSelectButton(controller.contextTopicsComboBox, selectedTopics, "Все темы");
        }
        if (controller.contextCategoryComboBox != null) {
            controller.contextCategoryComboBox.setValue(All.CATEGORIES);
        }
        if (controller.contextTagComboBox != null) {
            controller.contextTagComboBox.setValue(All.TAGS);
        }
        if (controller.contextDateFrom != null) {
            controller.contextDateFrom.setValue(null);
        }
        if (controller.contextDateTo != null) {
            controller.contextDateTo.setValue(null);
        }
        refreshEntityList();
        controller.setQueryContext(null);
    }

    private void setupListeners() {
        if (controller.contextDateFrom != null) {
            controller.contextDateFrom.valueProperty().addListener((observable, oldValue, newValue) -> updateQueryContext());
        }
        if (controller.contextDateTo != null) {
            controller.contextDateTo.valueProperty().addListener((observable, oldValue, newValue) -> updateQueryContext());
        }
        if (controller.contextCategoryComboBox != null) {
            controller.contextCategoryComboBox.setOnAction(event -> {
                String value = controller.contextCategoryComboBox.getValue();
                if (All.CATEGORIES.equals(value)) {
                    selectedCategory = null;
                } else {
                    selectedCategory = value;
                }
                refreshEntityList();
            });
        }
        if (controller.contextTagComboBox != null) {
            controller.contextTagComboBox.setOnAction(event -> {
                String value = controller.contextTagComboBox.getValue();
                if (All.TAGS.equals(value)) {
                    selectedTag = null;
                } else {
                    selectedTag = value;
                }
                refreshEntityList();
            });
        }
    }

    private void refreshUI() {
        refreshChats();
        refreshEntityFilters();
        refreshTopics();
        refreshEntityList();
        if (controller.contextDateFrom != null) {
            controller.contextDateFrom.setValue(queryContext == null ? null : queryContext.dateFrom());
        }
        if (controller.contextDateTo != null) {
            controller.contextDateTo.setValue(queryContext == null ? null : queryContext.dateTo());
        }
    }

    private void refreshChats() {
        if (controller.contextChatsComboBox == null) {
            return;
        }
        if (databaseContext == null || databaseContext.chats() == null) {
            controller.contextChatsComboBox.getItems().clear();
            return;
        }
        ObservableList<String> items = FXCollections.observableArrayList(databaseContext.chats().values());
        items.sort(String::compareToIgnoreCase);
        controller.contextChatsComboBox.getItems().setAll(items);
        selectedChats.retainAll(items);
        setupMultiSelectComboBox(controller.contextChatsComboBox, selectedChats, "Все каналы");
    }

    private Set<Long> getSelectedChatIds() {
        if (databaseContext == null || databaseContext.chats() == null) {
            return new HashSet<>();
        }
        return databaseContext.chats().entrySet().stream()
                .filter(entry -> selectedChats.contains(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private void refreshTopics() {
        if (controller.contextTopicsComboBox == null) {
            return;
        }
        if (databaseContext == null || databaseContext.topics() == null) {
            controller.contextTopicsComboBox.getItems().clear();
            return;
        }
        ObservableList<String> items = FXCollections.observableArrayList(databaseContext.topics());
        items.sort(String::compareToIgnoreCase);
        controller.contextTopicsComboBox.getItems().setAll(items);
        selectedTopics.retainAll(items);
        setupMultiSelectComboBox(controller.contextTopicsComboBox, selectedTopics, "Все темы");
    }

    private void refreshEntityFilters() {
        if (databaseContext == null || databaseContext.namedEntities() == null) {
            if (controller.contextCategoryComboBox != null) {
                controller.contextCategoryComboBox.getItems().clear();
            }
            if (controller.contextTagComboBox != null) {
                controller.contextTagComboBox.getItems().clear();
            }
            return;
        }
        if (controller.contextCategoryComboBox != null) {
            ObservableList<String> categories = FXCollections.observableArrayList();
            categories.add(All.CATEGORIES);
            databaseContext.namedEntities().stream()
                    .map(NamedEntity::getCategory)
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(categories::add);
            controller.contextCategoryComboBox.getItems().setAll(categories);
            if (selectedCategory != null && categories.contains(selectedCategory)) {
                controller.contextCategoryComboBox.setValue(selectedCategory);
            } else {
                selectedCategory = null;
                controller.contextCategoryComboBox.setValue(All.CATEGORIES);
            }
        }
        if (controller.contextTagComboBox != null) {
            ObservableList<String> tags = FXCollections.observableArrayList();
            tags.add(All.TAGS);
            databaseContext.namedEntities().stream()
                    .flatMap(entity -> entity.getTags() == null ? Stream.empty() : entity.getTags().stream())
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(tags::add);
            controller.contextTagComboBox.getItems().setAll(tags);
            if (selectedTag != null && tags.contains(selectedTag)) {
                controller.contextTagComboBox.setValue(selectedTag);
            } else {
                selectedTag = null;
                controller.contextTagComboBox.setValue(All.TAGS);
            }
        }
    }

    private void refreshEntityList() {
        if (controller.contextEntitiesListView == null) {
            return;
        }
        if (databaseContext == null || databaseContext.namedEntities() == null) {
            controller.contextEntitiesListView.getItems().clear();
            return;
        }
        ObservableList<NamedEntity> filtered = FXCollections.observableArrayList(
                databaseContext.namedEntities().stream()
                        .filter(this::matchesEntityFilters)
                        .sorted(Comparator.comparing(NamedEntity::getName, String.CASE_INSENSITIVE_ORDER))
                        .toList());
        controller.contextEntitiesListView.setItems(filtered);
        controller.contextEntitiesListView.setCellFactory(listView ->
                new ListCell<>() {
                    private final CheckBox checkBox = new CheckBox();

                    {
                        checkBox.setMouseTransparent(true);
                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    }

                    @Override
                    protected void updateItem(NamedEntity item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                            return;
                        }
                        checkBox.setText(formatEntity(item));
                        checkBox.setSelected(selectedEntities.contains(item));
                        setGraphic(checkBox);
                        setOnMousePressed(event -> {
                            if (event.getButton() != MouseButton.PRIMARY) {
                                return;
                            }
                            if (selectedEntities.contains(item)) {
                                selectedEntities.remove(item);
                            } else {
                                selectedEntities.add(item);
                            }
                            checkBox.setSelected(selectedEntities.contains(item));
                            updateQueryContext();
                            event.consume();
                        });
                    }
                }
        );
        controller.contextEntitiesListView.refresh();
        updateQueryContext();
    }

    private boolean matchesEntityFilters(NamedEntity entity) {
        if (selectedCategory != null && !selectedCategory.equals(entity.getCategory())) {
            return false;
        }
        if (selectedTag != null) {
            if (entity.getTags() == null || !entity.getTags().contains(selectedTag)) {
                return false;
            }
        }
        return true;
    }

    private String formatEntity(NamedEntity entity) {
        String name = entity.getName() == null ? "" : entity.getName();
        String category = entity.getCategory() == null ? "" : entity.getCategory();
        if (category.isBlank()) {
            return name;
        }
        return name + " — " + category;
    }

    public void selectAllFilteredEntities() {
        if (controller.contextEntitiesListView == null) {
            return;
        }
        for (NamedEntity entity : controller.contextEntitiesListView.getItems()) {
            if (!selectedEntities.contains(entity)) {
                selectedEntities.add(entity);
            }
        }
        controller.contextEntitiesListView.refresh();
        updateQueryContext();
    }

    public void clearFilteredEntities() {
        if (controller.contextEntitiesListView == null) {
            return;
        }
        selectedEntities.removeAll(controller.contextEntitiesListView.getItems());
        controller.contextEntitiesListView.refresh();
        updateQueryContext();
    }

    private void setupMultiSelectComboBox(ComboBox<String> comboBox, ObservableList<String> selectedItems, String promptText) {
        comboBox.setPromptText(promptText);
        comboBox.setCellFactory(listView ->
                new ListCell<>() {
                    private final CheckBox checkBox = new CheckBox();

                    {
                        checkBox.setMouseTransparent(true);
                        setContentDisplay(
                                ContentDisplay.GRAPHIC_ONLY
                        );
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                            return;
                        }
                        checkBox.setText(item);
                        checkBox.setSelected(selectedItems.contains(item));
                        setGraphic(checkBox);
                        setOnMousePressed(event -> {
                            if (event.getButton() != MouseButton.PRIMARY) {
                                return;
                            }
                            if (selectedItems.contains(item)) {
                                selectedItems.remove(item);
                            } else {
                                selectedItems.add(item);
                            }
                            checkBox.setSelected(selectedItems.contains(item));
                            refreshMultiSelectButton(comboBox, selectedItems, promptText);
                            updateQueryContext();
                            event.consume();
                            comboBox.show();
                        });
                    }
                }
        );
        comboBox.setButtonCell(
                new ListCell<>() {

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        refreshMultiSelectButton(this, selectedItems, promptText);
                    }
                }
        );
        comboBox.showingProperty().addListener(
                (observable, oldValue, showing) -> {
                    if (showing) {
                        comboBox.getItems().setAll(new ArrayList<>(comboBox.getItems()));
                    }
                }
        );
        refreshMultiSelectButton(comboBox, selectedItems, promptText);
    }

    private void refreshMultiSelectButton(ComboBox<String> comboBox, ObservableList<String> selectedItems, String promptText) {
        if (comboBox.getButtonCell() == null) {
            return;
        }
        refreshMultiSelectButton(comboBox.getButtonCell(), selectedItems, promptText);
    }

    private void refreshMultiSelectButton(ListCell<String> cell, ObservableList<String> selectedItems, String promptText) {
        if (selectedItems.isEmpty()) {
            cell.setText(promptText);
        } else {
            cell.setText("Выбрано: " + selectedItems.size());
        }
        cell.setGraphic(null);
    }

    private LocalDate getDateFrom() {
        return controller.contextDateFrom == null ? null : controller.contextDateFrom.getValue();
    }

    private LocalDate getDateTo() {
        return controller.contextDateTo == null ? null : controller.contextDateTo.getValue();
    }

    private void clearAvailableData() {
        if (controller.contextChatsComboBox != null) {
            controller.contextChatsComboBox.getItems().clear();
        }
        if (controller.contextTopicsComboBox != null) {
            controller.contextTopicsComboBox.getItems().clear();
        }
        if (controller.contextCategoryComboBox != null) {
            controller.contextCategoryComboBox.getItems().clear();
        }
        if (controller.contextTagComboBox != null) {
            controller.contextTagComboBox.getItems().clear();
        }
        if (controller.contextEntitiesListView != null) {
            controller.contextEntitiesListView.getItems().clear();
        }
    }
}