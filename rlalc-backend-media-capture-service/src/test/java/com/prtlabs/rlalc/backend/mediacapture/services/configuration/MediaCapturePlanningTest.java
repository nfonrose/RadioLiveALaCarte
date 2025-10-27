package com.prtlabs.rlalc.backend.mediacapture.services.configuration;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MediaCapturePlanning.
 */
public class MediaCapturePlanningTest {

    @Test
    public void testLoadFromFile() throws IOException {
        // Get the path to the configuration file
        String configPath = "src/main/resources/grvfm-backend-media-capture-service.conf";
        File configFile = new File(configPath);

        // Get the path to the alternate configuration file
        String alternateConfigPath = "src/main/resources/rlalc-media-capture-batch.conf";
        File alternateConfigFile = new File(alternateConfigPath);

        // Ensure at least one of the files exists
        assertTrue(configFile.exists() || alternateConfigFile.exists(), 
                  "Neither configuration file exists: " + configPath + " or " + alternateConfigPath);

        // Load the configuration
        MediaCapturePlanning planning = MediaCapturePlanning.fromFile(configFile, alternateConfigFile);

        // Verify the meta information
        assertNotNull(planning.getMeta(), "Meta information should not be null");
        assertEquals("urn:grvmf-gvfm-media-capture-service:media-capture-planning:1.0", 
                     planning.getMeta().getFormat(), "Format does not match expected value");

        // Verify the streams to capture
        List<MediaCapturePlanning.StreamToCapture> streams = planning.getStreamsToCapture();
        assertNotNull(streams, "Streams to capture should not be null");
        assertEquals(2, streams.size(), "Expected 2 streams to capture");

        // Verify the first stream
        MediaCapturePlanning.StreamToCapture stream1 = streams.get(0);
        assertEquals("France Inter", stream1.getTitle(), "First stream title does not match");
        assertEquals("7bf97a79-9612-411d-966b-657b6d77443e", stream1.getUuid(), "First stream UUID does not match");
        assertEquals("https://radiofrance.com/stream/franceinter", stream1.getStreamurl(), "First stream URL does not match");
        assertEquals("1761605906", stream1.getStartTimeUTCEpochSec(), "First stream start time does not match");
        assertEquals("1200", stream1.getDurationSeconds(), "First stream duration does not match");

        // Verify the second stream
        MediaCapturePlanning.StreamToCapture stream2 = streams.get(1);
        assertEquals("Europe 1", stream2.getTitle(), "Second stream title does not match");
        assertEquals("d84c5a96-5839-4d27-b033-c5f742832982", stream2.getUuid(), "Second stream UUID does not match");
        assertEquals("https://europe1.fr/stream", stream2.getStreamurl(), "Second stream URL does not match");
        assertEquals("1761605906", stream2.getStartTimeUTCEpochSec(), "Second stream start time does not match");
        assertEquals("1200", stream2.getDurationSeconds(), "Second stream duration does not match");
    }
}
