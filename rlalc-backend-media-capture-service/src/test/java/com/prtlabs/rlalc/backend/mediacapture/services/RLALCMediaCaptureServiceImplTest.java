package com.prtlabs.rlalc.backend.mediacapture.services;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import com.prtlabs.rlalc.backend.mediacapture.entrypoint.MediaCaptureServiceHK2Module;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.IRLALCMediaCaptureService;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.RLALCMediaCaptureServiceImpl;
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
        ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.bind(serviceLocator, new MediaCaptureServiceHK2Module());
        service = serviceLocator.getService(IRLALCMediaCaptureService.class);
    }

    @Test
    public void testStartMediaCapture() {
        // Simply verify that the start method doesn't throw any exceptions
        assertDoesNotThrow(() -> service.startMediaCapture());
    }
}
