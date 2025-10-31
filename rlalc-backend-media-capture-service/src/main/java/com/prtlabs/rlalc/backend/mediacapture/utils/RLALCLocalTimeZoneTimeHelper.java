package com.prtlabs.rlalc.backend.mediacapture.utils;

import com.prtlabs.utils.time.provider.IPrtTimeProviderService;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


public class RLALCLocalTimeZoneTimeHelper {

    @Inject IPrtTimeProviderService timeProviderService;

    /**
     * Build a string with the current time in the specified TimeZone
     * @param timeZone Example value: Europe/Paris, "America/New_York", ...
     */
    public String getCurrentDayForProgramAsYYYYMMDD(ZoneId timeZone) {
        ZonedDateTime localTimeForProgram = timeProviderService.nowAtTimeZone(timeZone);
        ZonedDateTime beginningOfLocalDayForProgram = localTimeForProgram.withHour(0).withMinute(0).withSecond(0).withNano(0);
        String currentDayForProgramAsYYYYMMDD = beginningOfLocalDayForProgram.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return currentDayForProgramAsYYYYMMDD;
    }

}
