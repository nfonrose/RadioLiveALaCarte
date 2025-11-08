package com.prtlabs.utils.config;

import java.io.File;

public class PrtConfigHelper {


    public static boolean isDevlocalNonDockerExecution() {
        // Detect where we are running
        String os = System.getProperty("os.name").toLowerCase();
        boolean isMac = os.contains("mac");
        boolean inDocker = new File("/.dockerenv").exists(); // Common Docker indicator

        boolean isDevLocalExecution = isMac && !inDocker;
        return isDevLocalExecution;
    }

}
