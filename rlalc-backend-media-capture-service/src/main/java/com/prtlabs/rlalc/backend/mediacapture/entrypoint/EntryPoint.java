package com.prtlabs.rlalc.backend.mediacapture.entrypoint;

import com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.IEmbeddedRESTServerModule;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.IRLALCMediaCaptureService;
import com.prtlabs.utils.logging.PrtLoggingUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
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

            // Create HK2 ServiceLocator
            ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
            ServiceLocatorUtilities.bind(serviceLocator, new MediaCaptureServiceHK2Module());

            // Get the service instance and start it
            IRLALCMediaCaptureService service = serviceLocator.getService(IRLALCMediaCaptureService.class);
            service.startMediaCapture();

            // Start the EmbeddedRESTServerModule
            PrtLoggingUtils.interceptJULLogsAndForwardToSLF4J();
            IEmbeddedRESTServerModule managementModule = serviceLocator.getService(IEmbeddedRESTServerModule.class);
            managementModule.start(9796, "RLALC Media Capture Server");

            logger.info(" -> RLALC Media Capture Service started successfully. On hold until killed");
        } catch (Exception e) {
            logger.error("  -> Error starting RLALC Media Capture Service", e);
            System.exit(1);
        }
    }

}
