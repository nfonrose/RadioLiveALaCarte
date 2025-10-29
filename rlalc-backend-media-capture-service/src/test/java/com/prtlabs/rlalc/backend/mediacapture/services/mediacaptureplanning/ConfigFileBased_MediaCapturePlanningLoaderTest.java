package com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.prtlabs.rlalc.backend.mediacapture.di.guice.MediaCaptureServiceGuiceModule;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.dto.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.loaders.file.ConfigFileBased_MediaCapturePlanningLoader;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MediaCapturePlanning.
 */
public class ConfigFileBased_MediaCapturePlanningLoaderTest {

    @Inject
    IMediaCapturePlanningLoader mediaCapturePlanningService;

    @BeforeEach
    public void setUp() {
        Injector injector = Guice.createInjector(new MediaCaptureServiceGuiceModule());
        mediaCapturePlanningService = injector.getInstance(IMediaCapturePlanningLoader.class);
    }

    @Test
    public void testLoadLoadMediaCapturePlanning() throws IOException {
        // This test is meant to test the "ConfigFileBased_MediaCapturePlanningLoader" planning loader
        assertEquals(mediaCapturePlanningService.getClass().getSimpleName(), ConfigFileBased_MediaCapturePlanningLoader.class.getSimpleName());

        // Load the configuration
        // REMARK: As the test is not overriding the default Guice injector, this code will load the MediaCapturePlanning from a file
        //         which is expected to be /Users/teevity/Dev/misc/@opt-prtlabs/radiolivealacarte/conf/rlalc-media-capture-batch-example001.conf
        //         if the 'prt.rlalc.baseDir' System property is set to '/Users/teevity/Dev/misc/@opt-prtlabs'
        MediaCapturePlanningDTO planning = mediaCapturePlanningService.loadMediaCapturePlanning();

        // Verify the meta information
        assertNotNull(planning.getMeta(), "Meta information should not be null");
        assertEquals("urn:grvmf-gvfm-media-capture-service:media-capture-planning:1.0", planning.getMeta().getFormat(), "Format does not match expected value");

        // Verify the streams to capture
        List<ProgramDescriptorDTO> streams = planning.getProgramsToCapture();
        assertNotNull(streams, "Streams to capture should not be null");
        assertEquals(2, streams.size(), "Expected 2 streams to capture");

        // Verify the first stream
        ProgramDescriptorDTO stream1 = streams.get(0);
        assertEquals("France Inter", stream1.getTitle(), "First stream title does not match");
        assertEquals("7bf97a79-9612-411d-966b-657b6d77443e", stream1.getUuid(), "First stream UUID does not match");
        assertEquals("http://direct.franceinter.fr/live/franceinter-midfi.mp3", stream1.getStreamURL(), "First stream URL does not match");
        assertEquals(1761605906, stream1.getStartTimeUTCEpochSec(), "First stream start time does not match");
        assertEquals(1200, stream1.getDurationSeconds(), "First stream duration does not match");

        // Verify the second stream
        ProgramDescriptorDTO stream2 = streams.get(1);
        assertEquals("Europe 1", stream2.getTitle(), "Second stream title does not match");
        assertEquals("d84c5a96-5839-4d27-b033-c5f742832982", stream2.getUuid(), "Second stream UUID does not match");
        assertEquals("https://europe1.lmn.fm/europe1.mp3", stream2.getStreamURL(), "Second stream URL does not match");
        assertEquals(1761605906, stream2.getStartTimeUTCEpochSec(), "Second stream start time does not match");
        assertEquals(1200, stream2.getDurationSeconds(), "Second stream duration does not match");
    }
}
