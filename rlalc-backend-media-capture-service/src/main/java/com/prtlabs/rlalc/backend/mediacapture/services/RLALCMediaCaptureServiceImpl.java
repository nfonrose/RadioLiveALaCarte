package com.prtlabs.rlalc.backend.mediacapture.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;

/**
 * Implementation of the RLALCMediaCaptureService.
 */
@Singleton
public class RLALCMediaCaptureServiceImpl implements RLALCMediaCaptureService {

    private static final Logger logger = LoggerFactory.getLogger(RLALCMediaCaptureServiceImpl.class);

    @Override
    public void start() {
        logger.info("Hello world");
    }
}
