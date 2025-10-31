package com.prtlabs.rlalc.backend.mediacapture.utils;

import com.prtlabs.utils.time.provider.IPrtTimeProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.*;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link RLALCLocalTimeZoneTimeHelper}.
 */
public class RLALCLocalTimeZoneTimeHelperTest {

    private RLALCLocalTimeZoneTimeHelper timeHelper;
    private MockTimeProviderService mockTimeProvider;

    /**
     * Custom mock implementation of IPrtTimeProviderService that returns a fixed time.
     */
    private static class MockTimeProviderService implements IPrtTimeProviderService {
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

    @BeforeEach
    public void setUp() throws Exception {
        // Create a fixed instant for testing (2023-07-15T12:30:45Z)
        Instant fixedInstant = Instant.parse("2023-07-15T12:30:45Z");
        mockTimeProvider = new MockTimeProviderService(fixedInstant);

        // Create an instance of RLALCLocalTimeZoneTimeHelper
        timeHelper = new RLALCLocalTimeZoneTimeHelper();

        // Use reflection to inject our mock
        Field field = RLALCLocalTimeZoneTimeHelper.class.getDeclaredField("timeProviderService");
        field.setAccessible(true);
        field.set(timeHelper, mockTimeProvider);
    }

    @Test
    public void testGetCurrentDayForProgramAsYYYYMMDD_CEST() {
        // Test with Europe/Paris timezone (CEST in July)
        ZoneId parisZone = ZoneId.of("Europe/Paris");
        String result = timeHelper.getCurrentDayForProgramAsYYYYMMDD(parisZone);

        // Expected: 2023-07-15 in Paris is "20230715"
        assertEquals("20230715", result);
    }

    @Test
    public void testGetCurrentDayForProgramAsYYYYMMDD_EST() {
        // Test with America/New_York timezone (EDT in July)
        ZoneId newYorkZone = ZoneId.of("America/New_York");
        String result = timeHelper.getCurrentDayForProgramAsYYYYMMDD(newYorkZone);

        // Expected: 2023-07-15 in New York is "20230715"
        assertEquals("20230715", result);
    }

    @Test
    public void testGetCurrentDayForProgramAsYYYYMMDD_JST() {
        // Test with Asia/Tokyo timezone (JST)
        ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");
        String result = timeHelper.getCurrentDayForProgramAsYYYYMMDD(tokyoZone);

        // Expected: 2023-07-15T12:30:45Z in Tokyo is 2023-07-15T21:30:45+09:00, so "20230715"
        assertEquals("20230715", result);
    }

    @Test
    public void testGetCurrentDayForProgramAsYYYYMMDD_DayBoundary() {
        // Set time to just before midnight in Paris
        // 2023-07-15T21:59:59Z is 2023-07-15T23:59:59+02:00 in Paris
        mockTimeProvider.setFixedInstant(Instant.parse("2023-07-15T21:59:59Z"));

        ZoneId parisZone = ZoneId.of("Europe/Paris");
        String result = timeHelper.getCurrentDayForProgramAsYYYYMMDD(parisZone);

        // Expected: Still 2023-07-15 in Paris
        assertEquals("20230715", result);

        // Now set time to just after midnight in Paris
        // 2023-07-15T22:00:01Z is 2023-07-16T00:00:01+02:00 in Paris
        mockTimeProvider.setFixedInstant(Instant.parse("2023-07-15T22:00:01Z"));

        result = timeHelper.getCurrentDayForProgramAsYYYYMMDD(parisZone);

        // Expected: Now it's 2023-07-16 in Paris
        assertEquals("20230716", result);
    }

    @Test
    public void testGetCurrentDayForProgramAsYYYYMMDD_DifferentDaysInDifferentTimezones() {
        // Set time to a moment when it's a different day in different timezones
        // 2023-07-15T22:30:00Z is:
        // - 2023-07-16T00:30:00+02:00 in Paris (next day)
        // - 2023-07-15T18:30:00-04:00 in New York (same day)
        mockTimeProvider.setFixedInstant(Instant.parse("2023-07-15T22:30:00Z"));

        ZoneId parisZone = ZoneId.of("Europe/Paris");
        String parisResult = timeHelper.getCurrentDayForProgramAsYYYYMMDD(parisZone);

        ZoneId newYorkZone = ZoneId.of("America/New_York");
        String newYorkResult = timeHelper.getCurrentDayForProgramAsYYYYMMDD(newYorkZone);

        // Expected: Different days in different timezones
        assertEquals("20230716", parisResult);
        assertEquals("20230715", newYorkResult);
    }

    @Test
    public void testGetCurrentDayForProgramAsYYYYMMDD_DaylightSavingTimeTransition() {
        // Test during DST transition in Europe/Paris
        // 2023-10-29T01:30:00Z is during the DST transition in Europe
        mockTimeProvider.setFixedInstant(Instant.parse("2023-10-29T01:30:00Z"));

        ZoneId parisZone = ZoneId.of("Europe/Paris");
        String result = timeHelper.getCurrentDayForProgramAsYYYYMMDD(parisZone);

        // Expected: 2023-10-29 in Paris
        assertEquals("20231029", result);
    }
}
