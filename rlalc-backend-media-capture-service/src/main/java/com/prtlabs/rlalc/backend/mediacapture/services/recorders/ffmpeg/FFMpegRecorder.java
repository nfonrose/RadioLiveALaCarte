package com.prtlabs.rlalc.backend.mediacapture.services.recorders.ffmpeg;

import com.prtlabs.rlalc.backend.mediacapture.services.recorders.IMediaRecorder;
import com.prtlabs.rlalc.domain.ProgramDescriptor;
import com.prtlabs.rlalc.domain.RecordingId;

import java.io.File;
import java.util.List;
import java.util.Map;


public class FFMpegRecorder implements IMediaRecorder {

    @Override
    public RecordingId record(ProgramDescriptor programDescriptor, Map<String, String> recorderSpecificParameters) {
        return null;
    }

    @Override
    public List<File> getChunkFilesForRecording(RecordingId recordingId) {
        return List.of();
    }

}
