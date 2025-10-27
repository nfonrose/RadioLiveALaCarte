package com.prtlabs.rlalc.backend.mediacapture.services;

import com.prtlabs.exceptions.PrtTechnicalException;

/**
 * Service for capturing media from radio broadcasts.
 */
public interface RLALCMediaCaptureService {
    
    /**
     * Starts the media capture service.
     */
    void start() throws PrtTechnicalException;

}