package com.ridelink.ride.service;

import com.ridelink.ride.dto.FareQuote;
import com.ridelink.ride.exception.ServiceException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
class FareValidatorTest {
    final FareValidator validator=new FareValidator();
    @Test void acceptsValidQuote() { assertThatCode(() -> validator.validate(new FareQuote(new BigDecimal("800.00"),"LKR"))).doesNotThrowAnyException(); }
    @Test void rejectsInvalidPriceAndCurrency() {
        for(var quote:new FareQuote[]{null,new FareQuote(null,"LKR"),new FareQuote(BigDecimal.ZERO,"LKR"),
            new FareQuote(new BigDecimal("1.234"),"LKR"),new FareQuote(BigDecimal.TEN,"USD"),new FareQuote(new BigDecimal("10000000000"),"LKR")})
            assertThatThrownBy(() -> validator.validate(quote)).isInstanceOf(ServiceException.class);
    }
}
