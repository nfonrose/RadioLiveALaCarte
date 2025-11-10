package com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.statemanagement.database.sqllite;

import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.statemanagement.IRecordingStateManagementService;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;

import java.io.File;
import java.util.List;
import java.util.Optional;


public class SQLliteBasedRecordingStateManagementService implements IRecordingStateManagementService {

    @Override
    public boolean createOrUpdateManifest(String outputDir, RecordingStatus.Status status, List<String> errors, List<File> chunkList) {
        return false;
    }

    @Override
    public void updateStatus(ProgramDescriptorDTO programDescriptor, Optional<Long> ffmpegProcessId, Optional<Integer> ffmpegProcessExitValue, String outputDirForRecordingChunks) {

    }

    @Override
    public void updateStatus(ProgramDescriptorDTO programDescriptor, Optional<Long> ffmpegProcessId, Optional<Integer> ffmpegProcessExitValue, String outputDirForRecordingChunks, List<String> ffmpegStdoutStdErrCapture) {

    }

    @Override
    public RecordingStatus readRecordingState(String outputDir) {
        return null;
    }

}
