package com.prtlabs.rlalc.backend.mediacapture.entrypoint;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.prtlabs.rlalc.backend.mediacapture.services.IRLALCMediaCaptureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Entry point for the RLALC Media Capture Service.
 */
public class EntryPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(EntryPoint.class);
    
    public static void main(String[] args) {
        try {
            logger.info("Starting RLALC Media Capture Service ...");
            Properties props = System.getProperties();

            // Create Guice injector
            Injector injector = Guice.createInjector(new MediaCaptureServiceGuiceModule());
            
            // Get the service instance and start it
            IRLALCMediaCaptureService service = injector.getInstance(IRLALCMediaCaptureService.class);
            service.startMediaCapture();
            
            logger.info(" -> RLALC Media Capture Service started successfully. On hold until killed");
        } catch (Exception e) {
            logger.error("  -> Error starting RLALC Media Capture Service", e);
            System.exit(1);
        }
    }

}