package com.prtlabs.rlalc.backend.mediacapture.entrypoint;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.IEmbeddedRESTServerModule;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.IRLALCMediaCaptureService;
import com.prtlabs.utils.logging.PrtLoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Entry point for the RLALC Media Capture Service.
 */
public class EntryPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(EntryPoint.class);
    
    public static void main(String[] args) {
        try {
            logger.info("Starting RLALC Media Capture Service ...");

            // Create Guice injector
            Injector injector = Guice.createInjector(new MediaCaptureServiceGuiceModule());
            
            // Get the service instance and start it
            IRLALCMediaCaptureService service = injector.getInstance(IRLALCMediaCaptureService.class);
            service.startMediaCapture();
            
            // Start the EmbeddedRESTServerModule
            PrtLoggingUtils.interceptJULLogsAndForwardToSLF4J();
            IEmbeddedRESTServerModule managementModule = injector.getInstance(IEmbeddedRESTServerModule.class);
            managementModule.start(9796, "RLALC Media Capture Server");

            logger.info(" -> RLALC Media Capture Service started successfully. On hold until killed");
        } catch (Exception e) {
            logger.error("  -> Error starting RLALC Media Capture Service", e);
            System.exit(1);
        }
    }

}