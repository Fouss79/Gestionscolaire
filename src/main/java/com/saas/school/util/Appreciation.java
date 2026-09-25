package com.saas.school.util;

import java.math.BigDecimal;

public final class Appreciation {

    private Appreciation() {
    }

    /** Les seuils sont exprimés sur 20 ; la note est ramenée sur 20 avant comparaison. */
    public static String de(BigDecimal note, int noteMax) {
        if (note == null) {
            return "";
        }
        double sur20 = note.doubleValue() * 20.0 / noteMax;
        if (sur20 >= 16) return "Très bien";
        if (sur20 >= 14) return "Bien";
        if (sur20 >= 12) return "Assez bien";
        if (sur20 >= 10) return "Passable";
        return "Insuffisant";
    }
}
