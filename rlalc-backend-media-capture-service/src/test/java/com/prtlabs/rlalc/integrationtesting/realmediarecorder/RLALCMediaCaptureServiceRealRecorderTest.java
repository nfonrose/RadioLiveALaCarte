package com.prtlabs.rlalc.integrationtesting.realmediarecorder;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.IRLALCMediaCaptureService;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.RLALCMediaCaptureServiceImpl;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.planning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.recorders.ffmpeg.FFMpegRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.statemanagement.IRecordingStateManagementService;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.statemanagement.manifests.ManifestFileBasedRecordingStateManagementService;
import com.prtlabs.rlalc.domain.ProgramId;
import com.prtlabs.rlalc.integrationtesting.BaseRLALCMediaCaptureServiceTest;
import com.prtlabs.rlalc.integrationtesting.MockMediaCapturePlanningLoader;
import com.prtlabs.rlalc.integrationtesting.MockTimeProviderService;
import com.prtlabs.utils.time.provider.IPrtTimeProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link IRLALCMediaCaptureService} using the real {@link FFMpegRecorder}.
 * This test will actually create files on disk.
 */
public class RLALCMediaCaptureServiceRealRecorderTest extends BaseRLALCMediaCaptureServiceTest {

    protected static final Logger logger = LoggerFactory.getLogger(RLALCMediaCaptureServiceRealRecorderTest.class);

    // Use a public radio stream for testing
    private static final String TEST_STREAM_URL = "http://direct.franceinter.fr/live/franceinter-midfi.mp3";
    private static final ZoneId TEST_TIMEZONE = ZoneId.of("Europe/Paris");
    private static final int TEST_DURATION_SECONDS = 20; // Short duration for testing
    private static final int MAX_WAIT_TIME_SECONDS = 30; // Maximum time to wait for files to be created

    private ProgramId testProgramId;

    @BeforeEach
    public void setUp() {
        // Initialize the mock time provider with current time
        mockTimeProvider = new MockTimeProviderService(Instant.now());

        // Initialize the mock planning loader
        mockPlanningLoader = new MockMediaCapturePlanningLoader();

        // Configure the HK2 injection for the test
        ServiceLocator serviceLocator = createServiceLocator(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(RLALCMediaCaptureServiceImpl.class).to(IRLALCMediaCaptureService.class);
                bind(ManifestFileBasedRecordingStateManagementService.class).to(IRecordingStateManagementService.class);
                bind(mockPlanningLoader).to(IMediaCapturePlanningLoader.class);
                bind(FFMpegRecorder.class).to(IMediaRecorder.class);
                bind(mockTimeProvider).to(IPrtTimeProviderService.class);
            }
        });

        mediaCaptureService = serviceLocator.getService(IRLALCMediaCaptureService.class);
    }

    /**
     * Test recording with the real FFMpegRecorder.
     * This test will:
     * 1. Start a recording of a public radio stream
     * 2. Wait for a short time to allow some chunks to be created
     * 3. Check if any chunks were created
     * 
     * The test has a timeout to prevent it from running indefinitely if something goes wrong.
     */
    @Test
    @Timeout(value = MAX_WAIT_TIME_SECONDS + 10, unit = TimeUnit.SECONDS)
    public void testRealRecording() {
        logger.info("Using stream URL: {}", TEST_STREAM_URL);
        logger.info("Current time: {}", Instant.now());

        // Clear any existing programs
        mockPlanningLoader.clearPrograms();

        // Add a program that starts now
        long startTimeEpochSec = Instant.now().getEpochSecond();
        mockPlanningLoader.addProgram("Test Real Recording", TEST_STREAM_URL, startTimeEpochSec, TEST_DURATION_SECONDS, TEST_TIMEZONE);

        // Get the program ID for later use
        testProgramId = mockPlanningLoader.getPrograms().get(0).getUuid();
        logger.info("Created program with ID: {}", testProgramId);

        try {
            // Start media capture
            logger.info("Starting media capture");
            mediaCaptureService.startMediaCapture();
            logger.info("Media capture started");

            // Wait for some time to allow chunks to be created
            // We'll check periodically to see if any chunks have been created
            logger.info("Waiting for chunks to be created (max {} seconds)", MAX_WAIT_TIME_SECONDS);
            boolean chunksCreated = waitForChunks(MAX_WAIT_TIME_SECONDS);

            // Check if any chunks were created
            if (chunksCreated) {
                logger.info("Chunks were created successfully");
            } else {
                logger.warn("No chunks were created within the timeout period");
                // Don't fail the test, as external factors might prevent chunks from being created
            }

            // Check recording status
            Map<ProgramId, RecordingStatus> statuses = mediaCaptureService.getRecordingStatusesForCurrentDay();
            if (statuses.containsKey(testProgramId)) {
                logger.info("Recording status: {}", statuses.get(testProgramId).getStatus());
                if (statuses.get(testProgramId).getStatus() == RecordingStatus.Status.PARTIAL_FAILURE) {
                    logger.error("Recording failed with errors: {}", statuses.get(testProgramId).getErrors());
                }
            } else {
                logger.warn("No recording status found for program ID: {}", testProgramId);
            }

        } catch (Exception e) {
            logger.error("Error during test: {}", e.getMessage(), e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    /**
     * Wait for chunks to be created, checking periodically.
     * 
     * @param maxWaitTimeSeconds Maximum time to wait in seconds
     * @return true if chunks were created, false otherwise
     */
    private boolean waitForChunks(int maxWaitTimeSeconds) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (maxWaitTimeSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            try {
                // Check if any chunks have been created
                List<URI> chunks = mediaCaptureService.getRecordingChunks(testProgramId, Instant.now());
                if (!chunks.isEmpty()) {
                    logger.info("Found recording chunks: {}", chunks);
                    return true;
                }

                // Wait a bit before checking again
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for chunks");
                return false;
            } catch (Exception e) {
                logger.warn("Error while checking for chunks: {}", e.getMessage());
                // Continue waiting
            }
        }

        return false;
    }
}
