package com.ridelink.farepayment;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class FareCalculatorTest {
    final FareCalculator calculator = new FareCalculator();
    @Test void calculatesDocumentedExample() {
        var result = calculator.calculate(new BigDecimal("10"), 20);
        assertThat(result.total()).isEqualByComparingTo("800.00");
        assertThat(result.currency()).isEqualTo("LKR");
        assertThat(result.ruleVersion()).isEqualTo("LKR-STANDARD-v1");
    }
    @Test void roundsCurrencyAndHandlesBounds() {
        assertThat(calculator.calculate(new BigDecimal("0.001"), 0).total()).isEqualByComparingTo("100.06");
        assertThat(calculator.calculate(new BigDecimal("1.234"), 1).total()).isEqualByComparingTo("179.04");
        assertThat(calculator.calculate(new BigDecimal("1000"), 1440).total()).isEqualByComparingTo("67300.00");
    }
    @Test void rejectsInvalidDistanceOrDuration() {
        for (String distance : new String[]{"0", "-1", "1000.001", "1.2345"})
            assertThatThrownBy(() -> calculator.calculate(new BigDecimal(distance), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(null, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(BigDecimal.ONE, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(BigDecimal.ONE, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(BigDecimal.ONE, 1441)).isInstanceOf(IllegalArgumentException.class);
    }
}
