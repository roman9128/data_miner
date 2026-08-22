package rt.telegram;

import java.io.File;

public class SessionEraser {
    public static void deleteSession() {
        File folder = new File("./session");
        deleteSession(folder);
    }

    private static void deleteSession(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteSession(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}