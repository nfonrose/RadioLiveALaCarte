package com.prtlabs.rlalc.backend.mediacapture.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Test for {@link RLALCMediaCaptureServiceImpl}.
 */
public class RLALCMediaCaptureServiceImplTest {
    
    private RLALCMediaCaptureService service;
    
    @BeforeEach
    public void setUp() {
        service = new RLALCMediaCaptureServiceImpl();
    }
    
    @Test
    public void testStart() {
        // Simply verify that the start method doesn't throw any exceptions
        assertDoesNotThrow(() -> service.start());
    }
}