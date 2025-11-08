package com.prtlabs.rlalc.integrationtesting;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.prtlabs.rlalc.backend.mediacapture.services.IRLALCMediaCaptureService;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.domain.ProgramId;
import com.prtlabs.utils.config.PrtConfigHelper;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.UUID;

/**
 * Base class for IRLALCMediaCaptureService integration tests.
 * Provides common setup and utility methods for test classes.
 */
public abstract class BaseRLALCMediaCaptureServiceTest {

    protected static final Logger logger = LoggerFactory.getLogger(BaseRLALCMediaCaptureServiceTest.class);

    protected IRLALCMediaCaptureService mediaCaptureService;
    protected MockTimeProviderService mockTimeProvider;
    protected MockMediaCapturePlanningLoader mockPlanningLoader;

    @BeforeAll
    static void setUpAll() {
        // Adjust the baseDir so that the test can run outside Docker on dev machines
        if (PrtConfigHelper.isDevlocalNonDockerExecution()) {
            System.setProperty("prt.rlalc.baseDir", "/Users/teevity/Dev/misc/@opt-prtlabs");
        }
    }

    /**
     * Creates a Guice injector with the specified module.
     * 
     * @param module The Guice module to use
     * @return The configured Guice injector
     */
    protected Injector createInjector(AbstractModule module) {
        return Guice.createInjector(module);
    }

    /**
     * Creates a program descriptor for testing.
     * 
     * @param title The program title
     * @param streamURL The stream URL
     * @param startTimeUTCEpochSec The start time in UTC epoch seconds
     * @param durationSeconds The duration in seconds
     * @param timeZone The time zone
     * @return A new ProgramDescriptorDTO
     */
    protected ProgramDescriptorDTO createProgramDescriptor(
            String title, 
            String streamURL, 
            long startTimeUTCEpochSec, 
            long durationSeconds, 
            ZoneId timeZone) {
        return new ProgramDescriptorDTO(
            new ProgramId(UUID.randomUUID().toString()),
            title,
            streamURL,
            startTimeUTCEpochSec,
            durationSeconds,
            timeZone
        );
    }
}
