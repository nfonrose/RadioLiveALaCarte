package com.prtlabs.rlalc.backend.mediacapture.services.recorders;

import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;

import java.util.Map;


public interface IMediaRecorder {

    String record(ProgramDescriptorDTO programDescriptor, Map<String, String> recorderSpecificParameters);
    RecordingStatus getChunkFilesForRecording(String recordingId);
    void stopRecording(String recordingId);

}
