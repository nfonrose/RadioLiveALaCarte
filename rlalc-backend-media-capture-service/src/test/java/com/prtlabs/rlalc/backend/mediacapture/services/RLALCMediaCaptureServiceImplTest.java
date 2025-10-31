package com.prtlabs.rlalc.backend.mediacapture.services;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.prtlabs.rlalc.backend.mediacapture.entrypoint.MediaCaptureServiceGuiceModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Test for {@link RLALCMediaCaptureServiceImpl}.
 */
public class RLALCMediaCaptureServiceImplTest {
    
    private IRLALCMediaCaptureService service;
    
    @BeforeEach
    public void setUp() {
        Injector injector = Guice.createInjector(new MediaCaptureServiceGuiceModule());
        service = injector.getInstance(IRLALCMediaCaptureService.class);
    }

    @Test
    public void testStartMediaCapture() {
        // Simply verify that the start method doesn't throw any exceptions
        assertDoesNotThrow(() -> service.startMediaCapture());
    }
}