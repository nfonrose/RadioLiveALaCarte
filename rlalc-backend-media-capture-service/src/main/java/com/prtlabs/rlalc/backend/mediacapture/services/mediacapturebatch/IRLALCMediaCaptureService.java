package com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch;

import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.domain.ProgramId;
import com.prtlabs.utils.exceptions.PrtTechnicalException;
import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for capturing media from radio broadcasts.
 */
public interface IRLALCMediaCaptureService {
    
    //
    // Starts the media capture service.
    //
    void startMediaCapture() throws PrtTechnicalException;

    //
    // Get the current status of the system and give access to the media chunks
    //
    List<ProgramId>                 getScheduledProgramIds();
    Map<ProgramId, RecordingStatus> getRecordingStatusesForCurrentDay();
    List<URI>                       getRecordingChunks(ProgramId programId, Instant day);

    ProgramId addOneTimeMediaCapture(ProgramDescriptorDTO programDescriptor);
}
