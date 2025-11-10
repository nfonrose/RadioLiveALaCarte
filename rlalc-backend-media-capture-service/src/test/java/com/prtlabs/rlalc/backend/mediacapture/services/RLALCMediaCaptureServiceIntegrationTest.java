package com.prtlabs.rlalc.backend.mediacapture.services;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.IRLALCMediaCaptureService;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.RLALCMediaCaptureServiceImpl;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.planning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.planning.loaders.codedefined.StaticallyDefined_MediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.recorders.ffmpeg.FFMpegRecorder;
import com.prtlabs.utils.config.PrtConfigHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;


/**
 * Integration test for {@link IRLALCMediaCaptureService} using the real {@link FFMpegRecorder}.
 */
public class RLALCMediaCaptureServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(RLALCMediaCaptureServiceIntegrationTest.class);

    private IRLALCMediaCaptureService mediaCaptureService;

    private long startTimeEpochSec;
    private long durationSeconds;

    @BeforeAll
    static void setUpAll() {
        // Adjust the baseDir so that the test can run outside Docker on our dev machines
        if (PrtConfigHelper.isDevlocalNonDockerExecution()) {
            System.setProperty("prt.rlalc.baseDir", "/Users/teevity/Dev/misc/@opt-prtlabs");    // Instead of /opt/prtlabs
        }
    }

    @BeforeEach
    public void setUp() {
        // Calculate start time (10 seconds before now) and duration (25 seconds total)
        startTimeEpochSec = Instant.now().getEpochSecond() - 10;
        durationSeconds = 25; // 10 seconds before now + 15 seconds after now = 25 seconds total

        logger.info("Setting up test with start time {} ({} seconds ago) and duration {} seconds",
                startTimeEpochSec, Instant.now().getEpochSecond() - startTimeEpochSec, durationSeconds);

        // Configure the HK2 injection for the test
        StaticallyDefined_MediaCapturePlanningLoader planningLoader = new StaticallyDefined_MediaCapturePlanningLoader(
            "France Inter",
            "http://direct.franceinter.fr/live/franceinter-midfi.mp3",
            startTimeEpochSec,
            durationSeconds,
            ZoneId.of("Europe/Paris"));

        ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.bind(serviceLocator, new AbstractBinder() {
            @Override
            protected void configure() {
                bind(RLALCMediaCaptureServiceImpl.class).to(IRLALCMediaCaptureService.class);
                bind(planningLoader).to(IMediaCapturePlanningLoader.class);
                bind(FFMpegRecorder.class).to(IMediaRecorder.class);
            }
        });
        mediaCaptureService = serviceLocator.getService(IRLALCMediaCaptureService.class);
    }

}
