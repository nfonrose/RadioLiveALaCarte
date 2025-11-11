package com.prtlabs.utils.logging;

import java.util.logging.Logger;
import java.util.logging.LogManager;
import org.slf4j.bridge.SLF4JBridgeHandler;


public class PrtLoggingUtils {

    /**
     *
     */
    public static void interceptJULLogsAndForwardToSLF4J() {
        // Remove default JUL handlers
        LogManager.getLogManager().reset();
        Logger rootLogger = Logger.getLogger("");
        for (var handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Bridge JUL → SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

}
