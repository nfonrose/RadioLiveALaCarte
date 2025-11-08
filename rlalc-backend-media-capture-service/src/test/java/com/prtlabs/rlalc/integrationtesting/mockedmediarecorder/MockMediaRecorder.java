package com.prtlabs.rlalc.integrationtesting.mockedmediarecorder;

import com.prtlabs.rlalc.backend.mediacapture.domain.RecordingStatus;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.recorders.IMediaRecorder;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.domain.ProgramId;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of IMediaRecorder for testing.
 */
class MockMediaRecorder implements IMediaRecorder {
    private final List<ProgramId> initializedPrograms = new ArrayList<>();
    private final List<ProgramId> startedPrograms = new ArrayList<>();
    private final Map<ProgramId, RecordingStatus> recordingStatuses = new ConcurrentHashMap<>();
    private final Map<ProgramId, List<File>> chunkFiles = new ConcurrentHashMap<>();

    @Override
    public void initBeforeRecording(ProgramDescriptorDTO programDescriptor) {
        initializedPrograms.add(programDescriptor.getUuid());
        recordingStatuses.put(programDescriptor.getUuid(), new RecordingStatus(RecordingStatus.Status.PENDING));
    }

    @Override
    public void startRecording(ProgramDescriptorDTO programDescriptor, Map<String, String> recorderSpecificParameters) {
        startedPrograms.add(programDescriptor.getUuid());
        recordingStatuses.put(programDescriptor.getUuid(), new RecordingStatus(RecordingStatus.Status.ONGOING));
    }

    @Override
    public void stopRecording(ProgramId programId) {
        recordingStatuses.put(programId, new RecordingStatus(RecordingStatus.Status.COMPLETED));
    }

    @Override
    public Map<ProgramId, RecordingStatus> getRecordingStatuses() {
        return new HashMap<>(recordingStatuses);
    }

    @Override
    public List<File> getChunkFiles(ProgramId programId, Instant day) {
        return chunkFiles.getOrDefault(programId, new ArrayList<>());
    }

    public List<ProgramId> getInitializedPrograms() {
        return new ArrayList<>(initializedPrograms);
    }

    public List<ProgramId> getStartedPrograms() {
        return new ArrayList<>(startedPrograms);
    }

    public void setRecordingStatus(ProgramId programId, RecordingStatus status) {
        recordingStatuses.put(programId, status);
    }

    public void setChunkFiles(ProgramId programId, List<File> files) {
        chunkFiles.put(programId, files);
    }
}
