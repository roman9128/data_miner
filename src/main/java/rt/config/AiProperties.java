package rt.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AiProperties {
    private static final Properties properties = new Properties();
    private static final String applicationPropertyFileName = "./ai.properties";
    private static String url;
    private static String key;

    static {
        try (InputStream inputStream = new FileInputStream(applicationPropertyFileName)) {
            properties.load(inputStream);
            url = properties.getProperty("url");
            key = properties.getProperty("key");
        } catch (IOException ex) {
            System.err.println("Ошибка при загрузке параметров для подключения ИИ-агента: " + ex);
        }
    }

    public static String getUrl() {
        return url;
    }

    public static String getKey() {
        return key;
    }
}