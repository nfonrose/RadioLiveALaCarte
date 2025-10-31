package com.prtlabs.rlalc.backend.mediacapture.services.recorders;

import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.domain.ProgramId;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;


public interface IMediaRecorder {

    void initBeforeRecording(ProgramDescriptorDTO programDescriptor, Map<String, String> recorderSpecificParameters);
    void startRecording(ProgramDescriptorDTO programDescriptor, Map<String, String> recorderSpecificParameters);
    void stopRecording(ProgramId programId);

    Map<ProgramId, RecordingStatus> getRecordingStatuses();
    List<File>                      getChunkFiles(ProgramId programId, Instant day);

}
