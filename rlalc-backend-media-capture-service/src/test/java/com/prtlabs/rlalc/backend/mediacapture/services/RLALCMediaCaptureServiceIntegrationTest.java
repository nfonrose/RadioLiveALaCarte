package com.prtlabs.rlalc.backend.mediacapture.services;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.IMediaCapturePlanningService;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.dto.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.backend.mediacapture.services.recorders.ffmpeg.FFMpegRecorder;
import com.prtlabs.utils.config.PrtConfigHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test for {@link IRLALCMediaCaptureService} using the real {@link FFMpegRecorder}.
 */
public class RLALCMediaCaptureServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(RLALCMediaCaptureServiceIntegrationTest.class);

    private IRLALCMediaCaptureService mediaCaptureService;


    @BeforeAll
    static void setUpAll() {
        // Adjust the baseDir so that the test can run outside Docker on our dev machines
        if (PrtConfigHelper.isDevlocalNonDockerExecution()) {
            System.setProperty("prt.rlalc.baseDir", "/Users/teevity/Dev/misc/@opt-prtlabs");    // Instead of /opt/prtlabs
        }
    }

    @BeforeEach
    public void setUp() {
        // Configure the Guice injection for the test
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IRLALCMediaCaptureService.class).to(RLALCMediaCaptureServiceImpl.class);
                bind(IMediaCapturePlanningService.class).toInstance(new IMediaCapturePlanningService() {
                    @Override
                    public MediaCapturePlanningDTO loadMediaCapturePlanning() {
                        // Build a MediaCapturePlanning
//                        return MediaCapturePlanningDTO.builder()
//                            .streamsToCapture(Arrays.asList(MediaCapturePlanningDTO.StreamToCapture.builder()
//                                .title("France Inter")
//                                .uuid(UUID.randomUUID().toString())
//                                .streamURL("")
//                                .startTimeUTCEpochSec("")
//                                .durationSeconds("")
//                                .build()))                           // Add Lombok to get the builders
//                            .build();
                        return null;
                    }
                });
                bind(IMediaRecorder.class).to(FFMpegRecorder.class);
            }
        });
        mediaCaptureService = injector.getInstance(IRLALCMediaCaptureService.class);
    }
}
