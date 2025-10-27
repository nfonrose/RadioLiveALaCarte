package com.prtlabs.rlalc.backend.mediacapture.entrypoint;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.prtlabs.rlalc.backend.mediacapture.di.guice.MediaCaptureServiceGuiceModule;
import com.prtlabs.rlalc.backend.mediacapture.services.RLALCMediaCaptureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the RLALC Media Capture Service.
 */
public class EntryPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(EntryPoint.class);
    
    public static void main(String[] args) {
        logger.info("Starting RLALC Media Capture Service ...");
        
        try {
            // Create Guice injector
            Injector injector = Guice.createInjector(new MediaCaptureServiceGuiceModule());
            
            // Get the service instance and start it
            RLALCMediaCaptureService service = injector.getInstance(RLALCMediaCaptureService.class);
            service.start();
            
            logger.info(" -> RLALC Media Capture Service started successfully");
        } catch (Exception e) {
            logger.error("  -> Error starting RLALC Media Capture Service", e);
            System.exit(1);
        }
    }

}