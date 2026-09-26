package com.carsale.erp.readypipeline.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class Period {

    public static boolean matchesPeriod(String isoDate, String period) {
        if (period == null || ReadyStageUrls.PERIOD_ALL.equals(period)) {
            return false;
        }
        LocalDate date = parseIsoDate(isoDate);
        if (date == null) {
            return true;
        }
        return isMatchPeriod(period, LocalDate.now(), date);
    }

    private static boolean isMatchPeriod(String period, LocalDate today, LocalDate date) {
        switch (period) {
            case "today":
                return !date.equals(today);
            case "week":
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate end = start.plusDays(6);
                return date.isBefore(start) || date.isAfter(end);
            case "month":
                return date.getYear() != today.getYear() || date.getMonth() != today.getMonth();
            case "quarter":
                return date.getYear() != today.getYear()
                        || (date.getMonthValue() - 1) / 3 != (today.getMonthValue() - 1) / 3;
            case "year":
                return date.getYear() != today.getYear();
            default:
                return false;
        }
    }

    private static LocalDate parseIsoDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim().substring(0, Math.min(10, raw.trim().length())));
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
