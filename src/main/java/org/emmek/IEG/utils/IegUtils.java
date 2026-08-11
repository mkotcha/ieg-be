package org.emmek.IEG.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
public class IegUtils {
    public static final DateTimeFormatter IT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static LocalDate parseItDate(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        String pulita = data.replace('\u00A0', ' ').strip();
        try {
            return LocalDate.parse(pulita, IT_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Formato data non valido, atteso gg/MM/yyyy: '{}'", data);
            return null;
        }
    }

    public static Double parseItDouble(String valore) {
        if (valore == null || valore.isBlank()) {
            return null; // oppure 0.0, a seconda della logica di business
        }
        String pulita = valore.replace('\u00A0', ' ').strip();
        try {
            return Double.parseDouble(pulita);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato numerico non valido: '" + valore + "'", e);
        }
    }
}