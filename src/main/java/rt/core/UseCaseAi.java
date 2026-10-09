package rt.core;

import rt.ai.Agent;
import rt.data_processing.embedder.EmbeddingClient;
import rt.data_processing.noun_extractor.NounExtractor;
import rt.model.ai.DatabaseContext;
import rt.model.ai.QueryContext;
import rt.model.ai.Usage;
import rt.model.notification.Notification;
import rt.notifier.Notifier;
import rt.storage.DatabaseManager;
import rt.view.main.MainView;

import java.util.concurrent.ExecutorService;

public class UseCaseAi implements AssistantAgent {

    private final Core core;
    private final MainView view;
    private final DatabaseManager db;
    private final Agent agent;
    private final ExecutorService executor;

    UseCaseAi(
            Core core,
            MainView view,
            DatabaseManager db,
            EmbeddingClient embeddingClient,
            NounExtractor nounExtractor,
            ExecutorService executor
    ) {
        this.core = core;
        this.view = view;
        this.db = db;
        this.executor = executor;
        this.agent = new Agent(db,embeddingClient, nounExtractor, this);
    }

    public boolean isThinking() {
        return agent.isThinking();
    }

    public DatabaseContext getDatabaseContext() {
        if (core.isBusy()) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Сейчас немного занят, подождите");
            return null;
        }
        return db.getDatabaseContext();
    }

    public void setQueryContext(QueryContext queryContext) {
        db.setQueryContext(queryContext);
    }

    public void clearChat() {
        agent.clearChat();
    }

    public void askAgent(String question) {
        if (core.isBusy()) {
            Notifier.instance().add(Notification.Level.SHOW_USER, "Сейчас немного занят, подождите");
            return;
        }
        executor.execute(() -> agent.ask(question));
    }

    @Override
    public void sendAnswer(String answer) {
        view.showAgentsAnswer(answer);
    }

    @Override
    public void sendUsage(Usage usage) {
        view.showTokenUsage(usage);
    }
}