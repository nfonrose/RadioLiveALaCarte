package com.prtlabs.rlalc.backend.mediacapture.services.recorders;

import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.domain.ProgramDescriptor;
import com.prtlabs.rlalc.domain.RecordingId;

import java.util.Map;


public interface IMediaRecorder {

    RecordingId record(ProgramDescriptor programDescriptor, Map<String, String> recorderSpecificParameters);
    RecordingStatus getChunkFilesForRecording(RecordingId recordingId);
    void stopRecording(RecordingId recordingId);

}
