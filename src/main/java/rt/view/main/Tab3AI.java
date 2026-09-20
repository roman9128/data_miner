package rt.view.main;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Tab3AI {
    private static final Tab3AI INSTANCE = new Tab3AI();
    private VBox thinkingMessage;
    private final Parser markdownParser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();
    private final HtmlRenderer markdownRenderer = HtmlRenderer.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

    private Tab3AI() {
    }

    static Tab3AI instance() {
        return INSTANCE;
    }

    void addAIMessage(VBox aiMessagesContainer, String message, boolean fromUser) {
        if (message == null || message.isBlank()) {
            return;
        }
        VBox messageBox = new VBox();
        messageBox.setCache(false);
        messageBox.setAlignment(Pos.CENTER_LEFT);
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
            setupAIMessageContextMenu(textArea);
            messageBox.getChildren().add(textArea);
        } else {
            WebView webView = createMarkdownWebView(message, aiMessagesContainer);
            webView.setCache(false);
            webView.setMaxWidth(Double.MAX_VALUE);
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
        thinkingMessage.setCache(false);
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

    void setupAIInput(Controller controller) {
        controller.aiInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.ENTER) {
                return;
            }
            if (event.isShiftDown()) {
                int caretPosition = controller.aiInput.getCaretPosition();
                controller.aiInput.insertText(caretPosition, "\n");
                controller.aiInput.positionCaret(caretPosition + 1);
                event.consume();
                return;
            }
            event.consume();
            if (!controller.aiInput.getText().isBlank()) {
                controller.onSendAIMessage();
            }
        });
    }

    void clearChat(VBox aiMessagesContainer) {
        aiMessagesContainer.getChildren().clear();
        thinkingMessage = null;
    }

    private WebView createMarkdownWebView(String markdown, VBox aiMessagesContainer) {
        WebView webView = new WebView();
        webView.setCache(false);
        webView.setContextMenuEnabled(false);
        webView.setFocusTraversable(true);
        webView.setMinWidth(0);
        webView.setPrefWidth(650);
        webView.setMaxWidth(Double.MAX_VALUE);
        webView.setMinHeight(40);
        webView.setPrefHeight(40);
        webView.setMaxHeight(Double.MAX_VALUE);
        webView.setStyle("-fx-background-color: transparent;");
        WebEngine engine = webView.getEngine();
        String html = renderMarkdown(markdown);
        engine.loadContent(html);
        webView.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() <= 0) {
                return;
            }
            Platform.runLater(() -> updateAIWebViewHeight(webView, engine, aiMessagesContainer));
        });
        engine.getLoadWorker().stateProperty().addListener(
                (obs, oldState, newState) -> {
                    if (newState != Worker.State.SUCCEEDED) {
                        return;
                    }
                    setupAIWebViewContextMenu(webView);
                    Platform.runLater(
                            () -> Platform.runLater(
                                    () -> updateAIWebViewHeight(webView, engine, aiMessagesContainer)));
                }
        );
        setupAIWebViewScroll(webView, aiMessagesContainer);
        return webView;
    }

    private void updateAIWebViewHeight(WebView webView, WebEngine engine, VBox aiMessagesContainer) {
        try {
            Number height = (Number) engine.executeScript("""
                    Math.ceil(
                        Math.max(
                            document.body.scrollHeight,
                            document.body.offsetHeight,
                            document.documentElement.scrollHeight,
                            document.documentElement.offsetHeight
                        )
                    )
                    """);
            if (height == null) {
                return;
            }
            double calculatedHeight = Math.max(40, height.doubleValue() + 4);
            webView.setPrefHeight(calculatedHeight);
            Platform.runLater(() -> scrollAIToBottom(aiMessagesContainer));

        } catch (Exception ignored) {
        }
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
            Parent parent = aiMessagesContainer.getParent();
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
                
                            /*
                             * WebView не должен иметь собственного скролла.
                             * Высота WebView будет равна высоте всего документа.
                             */
                            overflow: hidden;
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
                
                            /*
                             * Запрещаем внутреннюю прокрутку WebView.
                             */
                            overflow: hidden;
                
                            /*
                             * Текст всегда переносится внутри доступной ширины.
                             */
                            overflow-wrap: anywhere;
                            word-wrap: break-word;
                            word-break: break-word;
                        }
                
                        p {
                            margin: 0 0 10px 0;
                
                            overflow-wrap: anywhere;
                            word-wrap: break-word;
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
                            word-wrap: break-word;
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
                            word-wrap: break-word;
                            word-break: break-word;
                        }
                
                        blockquote {
                            margin: 10px 0;
                            padding: 7px 12px;
                
                            border-left: 3px solid #94a3b8;
                
                            background: #f8fafc;
                            color: #475569;
                
                            overflow-wrap: anywhere;
                            word-wrap: break-word;
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
                            word-wrap: break-word;
                            word-break: break-word;
                        }
                
                        pre {
                            margin: 10px 0;
                            padding: 12px 14px;
                
                            width: 100%;
                            max-width: 100%;
                
                            background: #f8fafc;
                
                            border: 1px solid #e2e8f0;
                            border-radius: 8px;
                
                            /*
                             * Код тоже переносится.
                             * Горизонтального скролла нет.
                             */
                            overflow: hidden;
                
                            white-space: pre-wrap;
                            overflow-wrap: anywhere;
                            word-wrap: break-word;
                            word-break: break-word;
                        }
                
                        pre code {
                            display: block;
                
                            padding: 0;
                
                            background: transparent;
                
                            border-radius: 0;
                
                            font-size: 13px;
                            color: #334155;
                
                            white-space: pre-wrap;
                
                            overflow-wrap: anywhere;
                            word-wrap: break-word;
                            word-break: break-word;
                        }
                
                        a {
                            color: #2563eb;
                
                            text-decoration: none;
                
                            overflow-wrap: anywhere;
                            word-wrap: break-word;
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
                
                            /*
                             * Таблица никогда не должна расширять WebView.
                             */
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
                
                            vertical-align: top;
                
                            overflow-wrap: anywhere;
                            word-wrap: break-word;
                            word-break: break-word;
                
                            white-space: normal;
                        }
                
                        td {
                            padding: 8px 10px;
                
                            border: 1px solid #e2e8f0;
                
                            color: #475569;
                
                            vertical-align: top;
                
                            overflow-wrap: anywhere;
                            word-wrap: break-word;
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
                
                        img {
                            max-width: 100%;
                            height: auto;
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
                "Собираю осколки смысла...",
                "Погружаюсь в глубины данных...",
                "Погнали...",
                "Вжух...",
                "Секунда...",
                "Уже...",
                "Пишу...",
                "Так... что тут у нас...",
                "Хм... сейчас разберёмся...",
                "Так, картина проясняется...",
                "Кажется, я понял...",
                "Начинаю складывать картину...",
                "Вот это уже интересно...",
                "Кажется, нашёл зацепку...",
                "Есть одна мысль...",
                "Кажется, есть решение...",
                "Проверю одну идею...",
                "Сейчас сведу всё воедино...",
                "Собираю картину целиком...",
                "Навожу порядок...",
                "Отделяю главное от второстепенного...",
                "Выцепляю главное...",
                "Смотрю, что здесь действительно важно...",
                "Проверяю логику...",
                "Не спешу с выводами...",
                "Секунду, хочу разобраться нормально...",
                "Лучше сначала проверю...",
                "Дай-ка посмотрю внимательнее...",
                "Похоже, тут есть нюанс...",
                "Нашёл интересный момент...",
                "Кажется, мы к чему-то пришли...",
                "Остался последний штрих...",
                "Советуюсь с внутренним экспертом...",
                "Проверяю по секретным источникам...",
                "Ищу ответ между строк...",
                "Допрашиваю базу данных...",
                "Провожу расследование...",
                "Веду переговоры с логикой...",
                "Убеждаю факты сотрудничать...",
                "Задаю неудобные вопросы данным...",
                "Пытаюсь договориться с математикой...",
                "Ищу второе дно...",
                "Проверяю дно первого дна...",
                "Разбираю матрёшку из смыслов...",
                "Собираю мысли в одну очередь...",
                "Ставлю мысли по местам...",
                "Провожу инвентаризацию мыслей...",
                "Заглядываю под капот...",
                "Открываю капот логики...",
                "Проверяю, что там шуршит...",
                "Что-то вычисляю. Наверное...",
                "Ситуация требует анализа...",
                "Это нужно обдумать...",
                "Проверяю все возможные варианты...",
                "Вопрос непростой...",
                "Есть над чем подумать...",
                "Подхожу к вопросу комплексно...",
                "Формирую стратегию...",
                "Выстраиваю причинно-следственные связи...",
                "Начинаю глубокий анализ...",
                "Провожу предварительное исследование...",
                "Оцениваю ситуацию...",
                "Изучаю вводные...",
                "Систематизирую полученную информацию...",
                "Пытаюсь понять масштаб проблемы...",
                "Разбираю ситуацию по косточкам...",
                "Делаю аналитическую паузу...",
                "Перехожу в режим эксперта...",
                "Включаю аналитический режим...",
                "Запускаю критическое мышление...",
                "Привлекаю тяжёлую артиллерию...",
                "Подключаю все доступные мощности...",
                "Задействую интеллектуальный резерв...",
                "Это заслуживает отдельного рассмотрения...",
                "Вопрос принят. Начинаю думать...",
                "Очень важный вопрос. Наверное...",
                "Сейчас будет серьёзно...",
                "Необходимо подойти к этому ответственно...",
                "Провожу совещание с самим собой..."
        };
        return thinkingLabels[ThreadLocalRandom.current().nextInt(thinkingLabels.length)];
    }
}