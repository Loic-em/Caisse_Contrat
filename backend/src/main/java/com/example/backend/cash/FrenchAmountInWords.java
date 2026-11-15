package com.example.backend.cash;

import java.math.BigDecimal;

public final class FrenchAmountInWords {

    private static final String[] SMALL = {
        "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
        "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize"
    };

    private static final String[] TENS = {
        "", "", "vingt", "trente", "quarante", "cinquante", "soixante"
    };

    private FrenchAmountInWords() {
    }

    public static String format(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Le montant en lettres exige un entier positif.");
        }

        long value;
        try {
            value = amount.longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Le montant dépasse la limite prise en charge.", exception);
        }
        if (value > 999_999_999_999L) {
            throw new IllegalArgumentException("Le montant dépasse la limite prise en charge.");
        }
        return number(value) + " francs CFA";
    }

    private static String number(long value) {
        if (value < 100) {
            return belowHundred((int) value);
        }
        if (value < 1_000) {
            int hundreds = (int) (value / 100);
            int remainder = (int) (value % 100);
            String result = hundreds == 1 ? "cent" : SMALL[hundreds] + " cent";
            if (remainder == 0) {
                return hundreds == 1 ? result : result + "s";
            }
            return result + " " + belowHundred(remainder);
        }
        if (value < 1_000_000) {
            long thousands = value / 1_000;
            long remainder = value % 1_000;
            String result = (thousands == 1 ? "" : numberBeforeMille(thousands) + " ") + "mille";
            return remainder == 0 ? result : result + " " + number(remainder);
        }
        if (value < 1_000_000_000) {
            return grouped(value, 1_000_000, "million");
        }
        return grouped(value, 1_000_000_000, "milliard");
    }

    private static String numberBeforeMille(long value) {
        String words = number(value);
        if (words.endsWith("cents") || words.endsWith("vingts")) {
            return words.substring(0, words.length() - 1);
        }
        return words;
    }

    private static String grouped(long value, long groupSize, String groupName) {
        long groups = value / groupSize;
        long remainder = value % groupSize;
        String result = number(groups) + " " + groupName + (groups == 1 ? "" : "s");
        return remainder == 0 ? result : result + " " + number(remainder);
    }

    private static String belowHundred(int value) {
        if (value < SMALL.length) {
            return SMALL[value];
        }
        if (value < 20) {
            return "dix-" + SMALL[value - 10];
        }
        if (value < 70) {
            int tens = value / 10;
            int units = value % 10;
            if (units == 0) {
                return TENS[tens];
            }
            return TENS[tens] + (units == 1 ? " et " : "-") + SMALL[units];
        }
        if (value < 80) {
            return value == 71 ? "soixante et onze" : "soixante-" + belowHundred(value - 60);
        }
        int remainder = value - 80;
        return remainder == 0 ? "quatre-vingts" : "quatre-vingt-" + belowHundred(remainder);
    }
}
