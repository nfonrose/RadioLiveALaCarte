package com.prtlabs.rlalc.backend.mediacapture.services;

import com.prtlabs.exceptions.PrtTechnicalException;
import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.domain.RecordingId;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for capturing media from radio broadcasts.
 */
public interface IRLALCMediaCaptureService {
    
    /**
     * Starts the media capture service.
     */
    void startMediaCapture() throws PrtTechnicalException;

    /**
     * Get the current status of the service
     */
    List<String> getScheduledProgramIds();
    Map<RecordingId, RecordingStatus> getRecordingStatuses();
    Map<RecordingId, RecordingStatus> getRecordingChunks(String programId, Instant day);

}