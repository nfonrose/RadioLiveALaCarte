package com.prtlabs.rlalc.backend.mediacapture.services.recorders;

import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;


public interface IMediaRecorder {

    String startRecording(ProgramDescriptorDTO programDescriptor, Map<String, String> recorderSpecificParameters);
    void stopRecording(String programId);

    List<RecordingStatus> getRecordingStatuses();
    List<File> getChunkFiles(String programId, Instant day);

}
