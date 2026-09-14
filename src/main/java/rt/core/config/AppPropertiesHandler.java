package rt.core.config;

import rt.model.notification.Notification;
import rt.core.Notifier;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppPropertiesHandler {

    private static final Properties properties = new Properties();
    private static final String appPropertyFileName = "./application.properties";
    private static int messagesToStop = 3000;
    private static int messagesToDownload = 100;

    static {
        try (InputStream inputStream = new FileInputStream(appPropertyFileName)) {
            properties.load(inputStream);
            messagesToStop = setMessageCountParameterFrom(properties.getProperty("stop"));
            messagesToDownload = setMessageCountParameterFrom(properties.getProperty("messages"));
        } catch (Exception e) {
            System.err.println("Не удалось загрузить параметры парсинга. Использую значения по умолчанию");
            createFileWithProperties();
        }
    }

    private AppPropertiesHandler() {
    }

    private static int setMessageCountParameterFrom(String count) {
        try {
            int param = Integer.parseInt(count);
            if (param > 100) {
                return param;
            } else return 100;
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    private static void createFileWithProperties() {
        try (FileWriter fileWriter = new FileWriter(appPropertyFileName, false)) {
            fileWriter.write(
                    "stop=" + messagesToStop + System.lineSeparator() +
                            "messages=" + messagesToDownload + System.lineSeparator());
        } catch (IOException e) {
            Notifier.instance().add(Notification.Level.ONLY_TO_LOG, "Не удалось создать файл с настройками по умолчанию. " + e.getMessage());
        }
    }

    public static int getMessagesToDownload() {
        return messagesToDownload;
    }

    public static int getMessagesToStop() {
        return messagesToStop;
    }
}