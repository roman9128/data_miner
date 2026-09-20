package rt.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Credentials {
    private static final Properties properties = new Properties();
    private static final String applicationPropertyFileName = "./credentials.properties";
    private static int apiID;
    private static String apiHash;
    private static String password;

    static {
        try (InputStream inputStream = new FileInputStream(applicationPropertyFileName)) {
            properties.load(inputStream);
            apiID = Integer.parseInt(properties.getProperty("api.ID"));
            apiHash = properties.getProperty("api.hash");
            password = properties.getProperty("password");
        } catch (IOException ex) {
            System.err.println("Ошибка при загрузке параметров для входа: " + ex);
        }
    }

    public static int getApiID() {
        return apiID;
    }

    public static String getApiHash() {
        return apiHash;
    }

    public static String getPassword() {
        return password;
    }
}