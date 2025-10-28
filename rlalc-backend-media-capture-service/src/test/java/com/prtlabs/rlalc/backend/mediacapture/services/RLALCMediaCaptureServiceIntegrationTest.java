package com.prtlabs.rlalc.backend.mediacapture.services;

import com.prtlabs.exceptions.PrtTechnicalException;
import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.backend.mediacapture.services.configuration.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.ffmpeg.FFMpegRecorder;
import com.prtlabs.rlalc.domain.ProgramDescriptor;
import com.prtlabs.rlalc.domain.RecordingId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link RLALCMediaCaptureService} using the real {@link FFMpegRecorder}.
 */
public class RLALCMediaCaptureServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(RLALCMediaCaptureServiceIntegrationTest.class);

    private RLALCMediaCaptureServiceImpl service;
    private FFMpegRecorder recorder;

    @BeforeAll
    static void setupLocalDevPath() {
        // Detect where we are running
        String os = System.getProperty("os.name").toLowerCase();
        boolean isMac = os.contains("mac");
        boolean inDocker = new File("/.dockerenv").exists(); // Common Docker indicator
        // When running directly on our Mac dev machines, use a DEVLOCAL path
        if (isMac && !inDocker) {
            System.setProperty("prt.rlalc.baseDir", "/Users/teevity/Dev/misc/@opt-prtlabs");    // Instead of /opt/prtlabs
        }
    }
}
