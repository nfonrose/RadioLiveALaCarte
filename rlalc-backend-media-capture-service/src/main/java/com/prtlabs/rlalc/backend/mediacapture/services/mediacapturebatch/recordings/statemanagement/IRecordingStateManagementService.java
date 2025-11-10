package com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.statemanagement;

import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;

import java.io.File;
import java.util.List;
import java.util.Optional;

public interface IRecordingStateManagementService {

    /**
     * Creates or updates the manifest file for a recording.
     *
     * @param outputDir the directory where the recording chunks are stored
     * @param status    the current status of the recording
     * @param errors    the list of errors (if any)
     * @param chunkList the list of chunk files
     * @return true if the manifest was successfully created/updated, false otherwise
     */
    public boolean createOrUpdateManifest(String outputDir, RecordingStatus.Status status, List<String> errors, List<File> chunkList);

    /**
     * Updates the status in the manifest file (without stdour/stderr capture)
     *
     * @param programDescriptor
     * @param ffmpegProcessId
     * @param ffmpegProcessExitValue
     * @param outputDirForRecordingChunks
     * @param ffmpegStdoutStdErrCapture
     * @return true if the status was successfully updated, false otherwise
     */
    public void updateStatus(ProgramDescriptorDTO programDescriptor, Optional<Long> ffmpegProcessId, Optional<Integer> ffmpegProcessExitValue, String outputDirForRecordingChunks);

    /**
     * Updates the status in the manifest file, with stdour/stderr capture
     *
     * @param programDescriptor
     * @param ffmpegProcessId
     * @param ffmpegProcessExitValue
     * @param outputDirForRecordingChunks
     * @param ffmpegStdoutStdErrCapture
     * @return true if the status was successfully updated, false otherwise
     */
    public void updateStatus(ProgramDescriptorDTO programDescriptor, Optional<Long> ffmpegProcessId, Optional<Integer> ffmpegProcessExitValue, String outputDirForRecordingChunks, List<String> ffmpegStdoutStdErrCapture);

    /**
     * Reads the recording state (from a "manifest file" or the db, ...) and returns a RecordingStatus object.
     *
     * @param outputDir the directory where the recording chunks are stored
     * @return a RecordingStatus object, or null if the manifest couldn't be read
     */
    public RecordingStatus readRecordingState(String outputDir);

}
