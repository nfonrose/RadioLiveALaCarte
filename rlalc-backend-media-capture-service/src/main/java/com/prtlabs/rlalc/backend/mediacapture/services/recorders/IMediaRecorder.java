package com.prtlabs.rlalc.backend.mediacapture.services.recorders;

import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.domain.ProgramDescriptor;
import com.prtlabs.rlalc.domain.RecordingId;

import java.util.Map;


public interface IMediaRecorder {

    String record(ProgramDescriptor programDescriptor, Map<String, String> recorderSpecificParameters);
    RecordingStatus getChunkFilesForRecording(String recordingId);
    void stopRecording(String recordingId);

}
