package com.prtlabs.rlalc.backend.mediacapture.services.recorders;

import com.prtlabs.rlalc.domain.ProgramDescriptor;
import com.prtlabs.rlalc.domain.RecordingId;

import java.io.File;
import java.util.List;
import java.util.Map;


public interface IMediaRecorder {

    RecordingId record(ProgramDescriptor programDescriptor, Map<String, String> recorderSpecificParameters);
    List<File> getChunkFilesForRecording(RecordingId recordingId);
    void stopRecording(RecordingId recordingId);

}
