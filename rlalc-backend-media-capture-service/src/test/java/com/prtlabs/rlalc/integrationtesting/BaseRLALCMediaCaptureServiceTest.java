package com.prtlabs.rlalc.integrationtesting;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.prtlabs.rlalc.backend.mediacapture.services.IRLALCMediaCaptureService;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.planning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.planning.dto.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.domain.ProgramId;
import com.prtlabs.utils.config.PrtConfigHelper;
import com.prtlabs.utils.time.provider.IPrtTimeProviderService;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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
     * Custom mock implementation of IPrtTimeProviderService that returns a fixed time.
     */
    protected static class MockTimeProviderService implements IPrtTimeProviderService {
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

    /**
     * Custom mock implementation of IMediaCapturePlanningLoader that allows dynamic configuration.
     */
    protected static class MockMediaCapturePlanningLoader implements IMediaCapturePlanningLoader {
        private List<ProgramDescriptorDTO> programs = new ArrayList<>();

        @Override
        public MediaCapturePlanningDTO loadMediaCapturePlanning() {
            return MediaCapturePlanningDTO.builder()
                .programsToCapture(programs)
                .build();
        }

        public void addProgram(String title, String streamURL, long startTimeUTCEpochSec, long durationSeconds, ZoneId timeZone) {
            ProgramId programId = new ProgramId(UUID.randomUUID().toString());
            programs.add(new ProgramDescriptorDTO(programId, title, streamURL, startTimeUTCEpochSec, durationSeconds, timeZone));
        }

        public void clearPrograms() {
            programs.clear();
        }

        public List<ProgramDescriptorDTO> getPrograms() {
            return new ArrayList<>(programs);
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
