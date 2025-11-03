package com.prtlabs.rlalc.integrationtesting;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.backend.mediacapture.services.IRLALCMediaCaptureService;
import com.prtlabs.rlalc.backend.mediacapture.services.RLALCMediaCaptureServiceImpl;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.planning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.recorders.IMediaRecorder;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.domain.ProgramId;
import com.prtlabs.utils.time.provider.IPrtTimeProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link IRLALCMediaCaptureService} using a mocked {@link IMediaRecorder}.
 */
public class RLALCMediaCaptureServiceMockTest extends BaseRLALCMediaCaptureServiceTest {

    private MockMediaRecorder mockMediaRecorder;
    private static final String TEST_STREAM_URL = "http://test-stream.mp3";
    private static final ZoneId TEST_TIMEZONE = ZoneId.of("Europe/Paris");

    @BeforeEach
    public void setUp() {
        // Initialize the mock time provider with current time
        mockTimeProvider = new MockTimeProviderService(Instant.now());
        
        // Initialize the mock planning loader
        mockPlanningLoader = new MockMediaCapturePlanningLoader();
        
        // Initialize the mock media recorder
        mockMediaRecorder = new MockMediaRecorder();
        
        // Configure the Guice injection for the test
        Injector injector = createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IRLALCMediaCaptureService.class).to(RLALCMediaCaptureServiceImpl.class);
                bind(IMediaCapturePlanningLoader.class).toInstance(mockPlanningLoader);
                bind(IMediaRecorder.class).toInstance(mockMediaRecorder);
                bind(IPrtTimeProviderService.class).toInstance(mockTimeProvider);
            }
        });
        
        mediaCaptureService = injector.getInstance(IRLALCMediaCaptureService.class);
    }
    
    @Test
    public void testStartMediaCapture_WithNoPrograms() {
        // Clear any existing programs
        mockPlanningLoader.clearPrograms();
        
        // Start media capture
        assertDoesNotThrow(() -> mediaCaptureService.startMediaCapture());
        
        // Verify no recordings were started
        assertTrue(mockMediaRecorder.getInitializedPrograms().isEmpty());
        assertTrue(mockMediaRecorder.getStartedPrograms().isEmpty());
    }
    
    @Test
    public void testStartMediaCapture_WithFutureProgram() {
        // Clear any existing programs
        mockPlanningLoader.clearPrograms();
        
        // Add a program that starts in the future
        long startTimeEpochSec = Instant.now().getEpochSecond() + 3600; // 1 hour in the future
        long durationSeconds = 1800; // 30 minutes
        mockPlanningLoader.addProgram("Test Program", TEST_STREAM_URL, startTimeEpochSec, durationSeconds, TEST_TIMEZONE);
        
        // Start media capture
        assertDoesNotThrow(() -> mediaCaptureService.startMediaCapture());
        
        // Verify no recordings were started (since the program is in the future)
        assertTrue(mockMediaRecorder.getInitializedPrograms().isEmpty());
        assertTrue(mockMediaRecorder.getStartedPrograms().isEmpty());
        
        // Verify the program is scheduled
        List<ProgramId> scheduledPrograms = mediaCaptureService.getScheduledProgramIds();
        assertEquals(1, scheduledPrograms.size());
    }
    
    @Test
    public void testStartMediaCapture_WithCurrentProgram() {
        // Clear any existing programs
        mockPlanningLoader.clearPrograms();
        
        // Add a program that is currently running
        long startTimeEpochSec = Instant.now().getEpochSecond() - 300; // 5 minutes ago
        long durationSeconds = 1800; // 30 minutes
        mockPlanningLoader.addProgram("Test Program", TEST_STREAM_URL, startTimeEpochSec, durationSeconds, TEST_TIMEZONE);
        
        // Start media capture
        assertDoesNotThrow(() -> mediaCaptureService.startMediaCapture());
        
        // Verify the recording was initialized and started
        assertEquals(1, mockMediaRecorder.getInitializedPrograms().size());
        assertEquals(1, mockMediaRecorder.getStartedPrograms().size());
        
        // Verify the program is scheduled
        List<ProgramId> scheduledPrograms = mediaCaptureService.getScheduledProgramIds();
        assertEquals(1, scheduledPrograms.size());
    }
    
    @Test
    public void testStartMediaCapture_WithPastProgram() {
        // Clear any existing programs
        mockPlanningLoader.clearPrograms();
        
        // Add a program that has already ended
        long startTimeEpochSec = Instant.now().getEpochSecond() - 7200; // 2 hours ago
        long durationSeconds = 1800; // 30 minutes
        mockPlanningLoader.addProgram("Test Program", TEST_STREAM_URL, startTimeEpochSec, durationSeconds, TEST_TIMEZONE);
        
        // Start media capture
        assertDoesNotThrow(() -> mediaCaptureService.startMediaCapture());
        
        // Verify no recordings were started (since the program has already ended)
        assertTrue(mockMediaRecorder.getInitializedPrograms().isEmpty());
        assertTrue(mockMediaRecorder.getStartedPrograms().isEmpty());
        
        // Verify no programs are scheduled
        List<ProgramId> scheduledPrograms = mediaCaptureService.getScheduledProgramIds();
        assertTrue(scheduledPrograms.isEmpty());
    }
    
    @Test
    public void testGetRecordingStatuses() {
        // Clear any existing programs
        mockPlanningLoader.clearPrograms();
        
        // Add a program
        long startTimeEpochSec = Instant.now().getEpochSecond() - 300; // 5 minutes ago
        long durationSeconds = 1800; // 30 minutes
        mockPlanningLoader.addProgram("Test Program", TEST_STREAM_URL, startTimeEpochSec, durationSeconds, TEST_TIMEZONE);
        
        // Start media capture
        assertDoesNotThrow(() -> mediaCaptureService.startMediaCapture());
        
        // Set a recording status in the mock recorder
        ProgramId programId = mockMediaRecorder.getStartedPrograms().get(0);
        RecordingStatus status = new RecordingStatus(RecordingStatus.Status.ONGOING);
        mockMediaRecorder.setRecordingStatus(programId, status);
        
        // Get recording statuses
        Map<ProgramId, RecordingStatus> statuses = mediaCaptureService.getRecordingStatuses();
        
        // Verify the status
        assertEquals(1, statuses.size());
        assertTrue(statuses.containsKey(programId));
        assertEquals(RecordingStatus.Status.ONGOING, statuses.get(programId).getStatus());
    }
    
    @Test
    public void testGetRecordingChunks() {
        // Clear any existing programs
        mockPlanningLoader.clearPrograms();
        
        // Add a program
        long startTimeEpochSec = Instant.now().getEpochSecond() - 300; // 5 minutes ago
        long durationSeconds = 1800; // 30 minutes
        mockPlanningLoader.addProgram("Test Program", TEST_STREAM_URL, startTimeEpochSec, durationSeconds, TEST_TIMEZONE);
        
        // Start media capture
        assertDoesNotThrow(() -> mediaCaptureService.startMediaCapture());
        
        // Set some chunk files in the mock recorder
        ProgramId programId = mockMediaRecorder.getStartedPrograms().get(0);
        List<File> chunkFiles = Arrays.asList(
            new File("/tmp/chunk1.mp3"),
            new File("/tmp/chunk2.mp3")
        );
        mockMediaRecorder.setChunkFiles(programId, chunkFiles);
        
        // Get recording chunks
        List<URI> chunks = mediaCaptureService.getRecordingChunks(programId, Instant.now());
        
        // Verify the chunks
        assertEquals(2, chunks.size());
        assertEquals(new File("/tmp/chunk1.mp3").getPath(), chunks.get(0).getPath());
        assertEquals(new File("/tmp/chunk2.mp3").getPath(), chunks.get(1).getPath());
    }
    
    /**
     * Mock implementation of IMediaRecorder for testing.
     */
    private static class MockMediaRecorder implements IMediaRecorder {
        private final List<ProgramId> initializedPrograms = new ArrayList<>();
        private final List<ProgramId> startedPrograms = new ArrayList<>();
        private final Map<ProgramId, RecordingStatus> recordingStatuses = new ConcurrentHashMap<>();
        private final Map<ProgramId, List<File>> chunkFiles = new ConcurrentHashMap<>();
        
        @Override
        public void initBeforeRecording(ProgramDescriptorDTO programDescriptor) {
            initializedPrograms.add(programDescriptor.getUuid());
            recordingStatuses.put(programDescriptor.getUuid(), new RecordingStatus(RecordingStatus.Status.PENDING));
        }
        
        @Override
        public void startRecording(ProgramDescriptorDTO programDescriptor, Map<String, String> recorderSpecificParameters) {
            startedPrograms.add(programDescriptor.getUuid());
            recordingStatuses.put(programDescriptor.getUuid(), new RecordingStatus(RecordingStatus.Status.ONGOING));
        }
        
        @Override
        public void stopRecording(ProgramId programId) {
            recordingStatuses.put(programId, new RecordingStatus(RecordingStatus.Status.COMPLETED));
        }
        
        @Override
        public Map<ProgramId, RecordingStatus> getRecordingStatuses() {
            return new HashMap<>(recordingStatuses);
        }
        
        @Override
        public List<File> getChunkFiles(ProgramId programId, Instant day) {
            return chunkFiles.getOrDefault(programId, new ArrayList<>());
        }
        
        public List<ProgramId> getInitializedPrograms() {
            return new ArrayList<>(initializedPrograms);
        }
        
        public List<ProgramId> getStartedPrograms() {
            return new ArrayList<>(startedPrograms);
        }
        
        public void setRecordingStatus(ProgramId programId, RecordingStatus status) {
            recordingStatuses.put(programId, status);
        }
        
        public void setChunkFiles(ProgramId programId, List<File> files) {
            chunkFiles.put(programId, files);
        }
    }
}