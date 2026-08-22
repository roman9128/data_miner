package rt.common.utils;

import java.util.Random;

public class NumberUtils {

    private static final Random random = new Random();

    private NumberUtils() {
    }

    public static int giveRandomNumber() {
        return random.nextInt(100, 500);
    }
}