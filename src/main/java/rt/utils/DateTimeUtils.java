package rt.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static long getUnixDateFrom(LocalDate dateFrom) {
        return dateFrom == null
                ? 0
                : dateFrom.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }

    public static long getUnixDateTo(LocalDate dateTo) {
        return dateTo == null
                ? Long.MAX_VALUE
                : dateTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    }

    public static LocalDateTime getDateTime(int unixTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTime), ZoneId.systemDefault());
    }

}