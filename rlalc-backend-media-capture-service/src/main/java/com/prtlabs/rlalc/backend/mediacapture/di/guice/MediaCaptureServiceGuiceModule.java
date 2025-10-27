package com.prtlabs.rlalc.backend.mediacapture.di.guice;

import com.google.inject.AbstractModule;
import com.prtlabs.rlalc.backend.mediacapture.services.RLALCMediaCaptureService;
import com.prtlabs.rlalc.backend.mediacapture.services.RLALCMediaCaptureServiceImpl;

/**
 * Guice module for the Media Capture service.
 */
public class MediaCaptureServiceGuiceModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(RLALCMediaCaptureService.class).to(RLALCMediaCaptureServiceImpl.class);
    }
}