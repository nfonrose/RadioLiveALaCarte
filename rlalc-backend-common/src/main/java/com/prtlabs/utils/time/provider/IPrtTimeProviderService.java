package com.prtlabs.utils.time.provider;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public interface IPrtTimeProviderService {
    Instant nowUTC();
    ZonedDateTime nowAtTimeZone(ZoneId timeZone);
}
