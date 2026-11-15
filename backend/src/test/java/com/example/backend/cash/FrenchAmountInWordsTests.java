package com.example.backend.cash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FrenchAmountInWordsTests {

    @Test
    void formatsFrenchAgreementAndCurrencyForCashVoucherAmounts() {
        assertEquals("quatre-vingts francs CFA", FrenchAmountInWords.format(new BigDecimal("80")));
        assertEquals("quatre-vingt-un francs CFA", FrenchAmountInWords.format(new BigDecimal("81")));
        assertEquals("deux cents francs CFA", FrenchAmountInWords.format(new BigDecimal("200")));
        assertEquals("deux cent un francs CFA", FrenchAmountInWords.format(new BigDecimal("201")));
        assertEquals("deux cent mille francs CFA", FrenchAmountInWords.format(new BigDecimal("200000")));
        assertEquals("quatre-vingt mille francs CFA", FrenchAmountInWords.format(new BigDecimal("80000")));
        assertEquals("deux cent quarante mille francs CFA",
                FrenchAmountInWords.format(new BigDecimal("240000")));
        assertEquals("un million francs CFA", FrenchAmountInWords.format(new BigDecimal("1000000")));
    }

    @Test
    void rejectsNonPositiveFractionalAndUnsupportedAmounts() {
        assertThrows(IllegalArgumentException.class,
                () -> FrenchAmountInWords.format(new BigDecimal("0")));
        assertThrows(IllegalArgumentException.class,
                () -> FrenchAmountInWords.format(new BigDecimal("1.50")));
        assertThrows(IllegalArgumentException.class,
                () -> FrenchAmountInWords.format(new BigDecimal("1000000000000")));
    }
}
