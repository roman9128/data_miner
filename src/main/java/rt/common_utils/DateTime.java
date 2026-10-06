package rt.common_utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTime {

    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    private DateTime() {
    }

    public static String getStringOf(LocalDateTime ldt) {
        try {
            return ldt.format(formatter);
        } catch (Exception e) {
            return "";
        }
    }

    public static LocalDateTime getLocalDateTimeOf(String dateTime) {
        try {
            return LocalDateTime.parse(dateTime, formatter);
        } catch (Exception e) {
            return LocalDateTime.now(ZoneId.systemDefault());
        }
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