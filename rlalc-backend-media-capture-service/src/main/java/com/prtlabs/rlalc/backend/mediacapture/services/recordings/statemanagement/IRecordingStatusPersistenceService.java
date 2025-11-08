package com.prtlabs.rlalc.backend.mediacapture.services.recordings.statemanagement;

import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;


public interface IRecordingStatusPersistenceService {

    void saveRecordingStatus(RecordingStatus status);

}
