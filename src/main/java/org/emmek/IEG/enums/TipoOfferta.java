package org.emmek.IEG.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TipoOfferta {
    PUN("PUN + SPREAD"),
    FISSO("PREZZO FISSO"),
    NEW("new");

    private final String value;

    public static TipoOfferta fromString(String s) {
        if (s == null || s.trim().isEmpty()) {
            return NEW;
        }
        String norm = s.trim();
        for (TipoOfferta t : values()) {
            if (t.getValue().equalsIgnoreCase(norm)) {
                return t;
            }
        }
        return NEW;
    }
}
