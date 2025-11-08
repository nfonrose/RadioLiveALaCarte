package com.prtlabs.rlalc.integrationtesting;

import com.prtlabs.utils.time.provider.IPrtTimeProviderService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Custom mock implementation of IPrtTimeProviderService that returns a fixed time.
 */
public class MockTimeProviderService implements IPrtTimeProviderService {

    private Instant fixedInstant;

    public MockTimeProviderService(Instant fixedInstant) {
        this.fixedInstant = fixedInstant;
    }

    @Override
    public Instant nowUTC() {
        return fixedInstant;
    }

    @Override
    public ZonedDateTime nowAtTimeZone(ZoneId timeZone) {
        return fixedInstant.atZone(timeZone);
    }

    public void setFixedInstant(Instant fixedInstant) {
        this.fixedInstant = fixedInstant;
    }

}
