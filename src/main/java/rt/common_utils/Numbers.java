package rt.common_utils;

import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

public final class Numbers {

    private static final Random random = new Random();

    private Numbers() {
    }

    public static int giveRandomNumber() {
        return random.nextInt(100, 500);
    }

    public static Set<Long> prepareSenderIds(Set<Long> source, Function<Integer, Collection<Long>> getFolder) {
        Set<Long> result = new TreeSet<>();
        source.forEach(n -> {
            if (n < 0) {
                result.add(n);
            } else if (n > 0) {
                result.addAll(getFolder.apply(n.intValue()));
            }
        });
        return result;
    }
}