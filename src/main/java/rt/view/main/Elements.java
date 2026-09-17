package rt.view.main;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class Elements {
    private static final Elements INSTANCE = new Elements();
    private VBox thinkingMessage;
    private final Parser markdownParser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();
    private final HtmlRenderer markdownRenderer = HtmlRenderer.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

    private Elements() {
    }

    public static Elements instance() {
        return INSTANCE;
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

    void setupMultiSelectComboBox(ComboBox<String> comboBox, ObservableList<String> selectedItems, String promptText) {
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

    void updateButtonCell(ComboBox<String> comboBox, ObservableList<String> selectedItems, String promptText) {
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

    void refreshComboBoxCells(ComboBox<String> comboBox) {
        comboBox.getItems().setAll(new ArrayList<>(comboBox.getItems()));
    }

    void addAIMessage(VBox aiMessagesContainer, String message, boolean fromUser) {
        if (message == null || message.isBlank()) {
            return;
        }
        VBox messageBox = new VBox();
        messageBox.setAlignment(fromUser ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        messageBox.getStyleClass().add(fromUser
                ? "ai-user-message-container"
                : "ai-agent-message-container");
        if (fromUser) {
            TextArea textArea = new TextArea(message);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setFocusTraversable(true);
            textArea.setMaxWidth(650);
            textArea.setPrefRowCount(calculateRows(message));
            textArea.setPrefHeight(calculateHeight(message));
            textArea.getStyleClass().add("ai-user-message");
            Elements.instance().setupAIMessageContextMenu(textArea);
            messageBox.getChildren().add(textArea);
        } else {
            WebView webView = createMarkdownWebView(message, aiMessagesContainer);
            messageBox.getChildren().add(webView);
        }
        aiMessagesContainer.getChildren().add(messageBox);
        scrollAIToBottom(aiMessagesContainer);
    }

    void showThinkingIndicator(VBox aiMessagesContainer) {
        if (thinkingMessage != null) {
            return;
        }
        Label label = new Label(getRandomThinkingLabel());
        label.getStyleClass().add("ai-thinking-text");
        label.setPadding(new Insets(10, 14, 10, 14));
        thinkingMessage = new VBox(label);
        thinkingMessage.setAlignment(Pos.CENTER_LEFT);
        thinkingMessage.getStyleClass().add("ai-thinking-message");
        aiMessagesContainer.getChildren().add(thinkingMessage);
        scrollAIToBottom(aiMessagesContainer);
    }

    void hideThinkingIndicator(VBox aiMessagesContainer) {
        if (thinkingMessage == null) {
            return;
        }
        aiMessagesContainer.getChildren().remove(thinkingMessage);
        thinkingMessage = null;
    }

    private WebView createMarkdownWebView(String markdown, VBox aiMessagesContainer) {
        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.setFocusTraversable(true);
        webView.setPrefWidth(650);
        webView.setMaxWidth(650);
        webView.setMinHeight(40);
        webView.setPrefHeight(80);
        webView.setStyle("-fx-background-color: transparent;");
        setupAIWebViewScroll(webView, aiMessagesContainer);
        WebEngine engine = webView.getEngine();
        String html = renderMarkdown(markdown);
        engine.loadContent(html);
        engine.getLoadWorker().stateProperty().addListener(
                (obs, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        Number height = (Number) engine.executeScript("document.documentElement.scrollHeight");
                        if (height != null) {
                            double calculatedHeight = height.doubleValue() + 4;
                            webView.setPrefHeight(Math.max(40, calculatedHeight));
                        }
                        setupAIWebViewContextMenu(webView);
                        Platform.runLater(() -> scrollAIToBottom(aiMessagesContainer));
                    }
                }
        );
        return webView;
    }

    private void scrollAIToBottom(VBox aiMessagesContainer) {
        Platform.runLater(() -> {
            if (aiMessagesContainer.getParent() instanceof ScrollPane scrollPane) {
                scrollPane.setVvalue(1.0);
            }
        });
    }

    private void setupAIWebViewContextMenu(WebView webView) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copy = new MenuItem("Копировать");
        MenuItem copyAll = new MenuItem("Копировать всё");
        MenuItem selectAll = new MenuItem("Выделить всё");
        copy.setOnAction(event -> {
            Object selectedText = webView.getEngine().executeScript("window.getSelection().toString()");
            if (selectedText instanceof String text && !text.isEmpty()) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                clipboard.setContent(content);
            }
        });

        copyAll.setOnAction(event -> {
            Object allText = webView.getEngine().executeScript("document.body.innerText");
            if (allText instanceof String text && !text.isEmpty()) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                clipboard.setContent(content);
            }
        });
        selectAll.setOnAction(event -> {
            webView.getEngine().executeScript("""
                        const selection = window.getSelection();
                        const range = document.createRange();
                    
                        range.selectNodeContents(document.body);
                    
                        selection.removeAllRanges();
                        selection.addRange(range);
                    """);
            webView.requestFocus();
        });
        contextMenu.getItems().addAll(copy, copyAll, selectAll);
        webView.setOnContextMenuRequested(event -> {
            contextMenu.show(webView, event.getScreenX(), event.getScreenY());
        });
    }

    private void setupAIWebViewScroll(WebView webView, VBox aiMessagesContainer) {
        webView.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) {
                return;
            }
            Parent parent = webView.getParent();
            while (parent != null && !(parent instanceof ScrollPane)) {
                parent = parent.getParent();
            }
            if (!(parent instanceof ScrollPane scrollPane)) {
                return;
            }
            double contentHeight = aiMessagesContainer.getBoundsInLocal().getHeight();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();
            double scrollableHeight = contentHeight - viewportHeight;
            if (scrollableHeight <= 0) {
                return;
            }
            double delta = event.getDeltaY() / scrollableHeight;
            double newValue = scrollPane.getVvalue() - delta;
            scrollPane.setVvalue(Math.max(0, Math.min(1, newValue)));
            event.consume();
        });
    }

    private void setupAIMessageContextMenu(TextArea textArea) {
        MenuItem copy = new MenuItem("Копировать");
        copy.setOnAction(event -> {
            String selected = textArea.getSelectedText();
            if (selected == null || selected.isEmpty()) {
                return;
            }
            ClipboardContent content = new ClipboardContent();
            content.putString(selected);
            Clipboard.getSystemClipboard().setContent(content);
        });
        MenuItem copyAll = new MenuItem("Копировать всё");
        copyAll.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(textArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        MenuItem selectAll = new MenuItem("Выделить всё");
        selectAll.setOnAction(event -> textArea.selectAll());

        ContextMenu contextMenu = new ContextMenu(copy, copyAll, new SeparatorMenuItem(), selectAll);
        textArea.setContextMenu(contextMenu);
    }

    private String renderMarkdown(String markdown) {
        Node document = markdownParser.parse(markdown);
        String body = markdownRenderer.render(document);

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                
                    <style>
                        * {
                            box-sizing: border-box;
                        }
                
                        html {
                            width: 100%;
                            max-width: 100%;
                
                            margin: 0;
                            padding: 0;
                
                            background: transparent;
                
                            overflow-x: hidden;
                        }
                
                        body {
                            width: 100%;
                            max-width: 100%;
                
                            margin: 0;
                            padding: 0;
                
                            background: transparent;
                
                            font-family: "Segoe UI", Arial, sans-serif;
                            font-size: 14px;
                            line-height: 1.55;
                
                            color: #1e293b;
                
                            overflow-x: hidden;
                
                            overflow-wrap: anywhere;
                            word-wrap: break-word;
                            word-break: break-word;
                        }
                
                        p {
                            margin: 0 0 10px 0;
                
                            overflow-wrap: anywhere;
                            word-break: break-word;
                        }
                
                        p:last-child {
                            margin-bottom: 0;
                        }
                
                        h1,
                        h2,
                        h3,
                        h4,
                        h5,
                        h6 {
                            color: #0f172a;
                
                            margin-top: 14px;
                            margin-bottom: 8px;
                
                            line-height: 1.3;
                
                            overflow-wrap: anywhere;
                            word-break: break-word;
                        }
                
                        h1 {
                            font-size: 21px;
                        }
                
                        h2 {
                            font-size: 19px;
                        }
                
                        h3 {
                            font-size: 17px;
                        }
                
                        h4,
                        h5,
                        h6 {
                            font-size: 15px;
                        }
                
                        ul,
                        ol {
                            margin-top: 6px;
                            margin-bottom: 10px;
                
                            padding-left: 24px;
                        }
                
                        li {
                            margin-bottom: 4px;
                
                            overflow-wrap: anywhere;
                            word-break: break-word;
                        }
                
                        blockquote {
                            margin: 10px 0;
                            padding: 7px 12px;
                
                            border-left: 3px solid #94a3b8;
                
                            background: #f8fafc;
                            color: #475569;
                
                            overflow-wrap: anywhere;
                            word-break: break-word;
                        }
                
                        code {
                            font-family: Consolas, "Courier New", monospace;
                            font-size: 13px;
                
                            background: #f1f5f9;
                            color: #334155;
                
                            padding: 2px 5px;
                
                            border-radius: 4px;
                
                            overflow-wrap: anywhere;
                            word-break: break-word;
                        }
                
                        pre {
                            margin: 10px 0;
                            padding: 12px 14px;
                
                            max-width: 100%;
                
                            background: #f8fafc;
                
                            border: 1px solid #e2e8f0;
                            border-radius: 8px;
                
                            overflow-x: hidden;
                
                            white-space: pre-wrap;
                            overflow-wrap: anywhere;
                            word-break: break-word;
                        }
                
                        pre code {
                            padding: 0;
                
                            background: transparent;
                
                            border-radius: 0;
                
                            font-size: 13px;
                            color: #334155;
                
                            white-space: pre-wrap;
                            overflow-wrap: anywhere;
                            word-break: break-word;
                        }
                
                        a {
                            color: #2563eb;
                
                            text-decoration: none;
                
                            overflow-wrap: anywhere;
                            word-break: break-word;
                        }
                
                        a:hover {
                            text-decoration: underline;
                        }
                
                        hr {
                            border: 0;
                
                            border-top: 1px solid #e2e8f0;
                
                            margin: 14px 0;
                        }
                
                        table {
                            width: 100%;
                            max-width: 100%;
                
                            table-layout: fixed;
                
                            border-collapse: collapse;
                
                            margin: 12px 0;
                
                            font-size: 13px;
                
                            border: 1px solid #dbe2ea;
                        }
                
                        thead {
                            background: #f8fafc;
                        }
                
                        th {
                            text-align: left;
                
                            font-weight: 600;
                
                            color: #334155;
                
                            padding: 8px 10px;
                
                            border: 1px solid #dbe2ea;
                
                            overflow-wrap: anywhere;
                            word-break: break-word;
                
                            white-space: normal;
                        }
                
                        td {
                            padding: 8px 10px;
                
                            border: 1px solid #e2e8f0;
                
                            color: #475569;
                
                            vertical-align: top;
                
                            overflow-wrap: anywhere;
                            word-break: break-word;
                
                            white-space: normal;
                        }
                
                        tbody tr:nth-child(even) {
                            background: #fafcff;
                        }
                
                        strong {
                            color: #0f172a;
                        }
                
                        em {
                            color: #475569;
                        }
                
                        del {
                            color: #64748b;
                        }
                    </style>
                </head>
                
                <body>
                    %s
                </body>
                </html>
                """.replace("%s", body);
    }

    private int calculateRows(String message) {
        if (message == null || message.isEmpty()) {
            return 1;
        }
        String[] lines = message.split("\n", -1);
        int rows = 0;
        for (String line : lines) {
            int length = line.length();
            rows += Math.max(1, (int) Math.ceil(length / 75.0));
        }
        return Math.max(1, Math.min(rows, 30));
    }

    private double calculateHeight(String message) {
        int rows = calculateRows(message);
        return Math.min(500, Math.max(45, rows * 20.0 + 24));
    }

    private String getRandomThinkingLabel() {
        final String[] thinkingLabels = {
                "Думаю...",
                "Призадумался...",
                "Скоро вернусь с ответом",
                "Чешу репу...",
                "Шевелю извилинами...",
                "Озадачился...",
                "Напрягаю мозг...",
                "Готовлю ответ...",
                "Что-то в этом есть",
                "Ищу смысл...",
                "Компилирую мысль...",
                "Обрабатываю запрос...",
                "Гружу нейроны...",
                "Оптимизирую решение...",
                "Синхронизирую полушария...",
                "Собираю пазл...",
                "Ищу баг в логике...",
                "Рендерю ответ...",
                "Загружаю данные...",
                "Кэширую идею...",
                "Так, секундочку...",
                "Дай подумать...",
                "Сейчас что-нибудь придумаем...",
                "Мозг кипит...",
                "Ага, понял, размышляю...",
                "Секунду, почти готово...",
                "Взвешиваю все за и против...",
                "Хм, интересная задачка...",
                "Кручу-верчу, запутать хочу (нет)...",
                "Прикидываю варианты...",
                "Бужу нейросеть...",
                "Подкидываю монетку...",
                "Советуюсь с электронным ветром...",
                "Делаю вид, что работаю...",
                "Генерирую гениальность...",
                "Считаю до бесконечности...",
                "Пытаюсь не зависнуть...",
                "Распутываю клубок...",
                "Ловлю мысль за хвост...",
                "Настраиваю волну...",
                "Просеиваю информацию...",
                "Зажигаю искру...",
                "Собираю осколки смысла...",
                "Погружаюсь в глубины данных...",
                "Погнали...",
                "Вжух...",
                "Секунда...",
                "Уже...",
                "Пишу..."
        };
        return thinkingLabels[ThreadLocalRandom.current().nextInt(thinkingLabels.length)];
    }
}
