package rt.core;

import rt.data_processing.DataProcessor;
import rt.data_processing.embedder.EmbeddingClient;
import rt.data_processing.noun_extractor.NounExtractor;
import rt.storage.DatabaseManager;
import rt.view.main.MainView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Core {

    private final MainView view;
    private final DataProcessor dataProcessor;
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    private final UseCaseAi useCaseAi;
    private final UseCaseCsvExport useCaseCsvExport;
    private final UseCaseTelegram useCaseTelegram;

    public Core() {
        this.view = new MainView();
        DatabaseManager db = new DatabaseManager();
        EmbeddingClient embeddingClient = new EmbeddingClient();
        NounExtractor nounExtractor = new NounExtractor();
        this.dataProcessor = new DataProcessor(db, embeddingClient, nounExtractor);

        this.useCaseAi = new UseCaseAi(this, view, db, embeddingClient, nounExtractor, executor);
        this.useCaseCsvExport = new UseCaseCsvExport(this, db);
        this.useCaseTelegram = new UseCaseTelegram(this, executor, dataProcessor);

        view.setCore(this);
        view.setUseCases(useCaseAi, useCaseCsvExport, useCaseTelegram);
    }

    public void init() {
        executor.execute(view::start);
        executor.execute(useCaseTelegram::start);
        executor.execute(dataProcessor::start);
    }

    public boolean isBusy() {
        return !dataProcessor.queueIsEmpty() || useCaseCsvExport.isExporting() || useCaseTelegram.isParsing();
    }

    public void closeApp() {
        useCaseTelegram.stop();
        dataProcessor.stop();
        executor.shutdown();
    }
}