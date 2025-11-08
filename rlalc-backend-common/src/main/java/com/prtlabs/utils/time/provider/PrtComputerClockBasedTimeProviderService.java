package com.prtlabs.utils.time.provider;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class PrtComputerClockBasedTimeProviderService implements IPrtTimeProviderService {

    @Override
    public Instant nowUTC() {
        return Instant.now();
    }

    @Override
    public ZonedDateTime nowAtTimeZone(ZoneId timeZone) {
        return Instant.now().atZone(timeZone);
    }

}
