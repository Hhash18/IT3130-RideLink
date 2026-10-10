package com.ridelink.ride.service;


import com.ridelink.ride.dto.*;
import com.ridelink.ride.exception.ServiceException;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
@Component
public class FareValidator {
    public void validate(FareQuote quote) {
        if(quote==null || quote.total()==null || quote.total().signum()<=0
            || quote.total().compareTo(new BigDecimal("9999999999.99"))>0 || quote.total().stripTrailingZeros().scale()>2
            || !"LKR".equals(quote.currency())) throw ServiceException.invalid("Fare");
    }
}
