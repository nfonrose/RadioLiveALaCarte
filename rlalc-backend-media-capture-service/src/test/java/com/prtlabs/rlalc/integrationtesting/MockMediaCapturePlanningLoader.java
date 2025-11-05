package com.prtlabs.rlalc.integrationtesting;

import com.prtlabs.rlalc.backend.mediacapture.services.recordings.planning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.planning.dto.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.domain.ProgramId;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Custom mock implementation of IMediaCapturePlanningLoader that allows dynamic configuration.
 */
public class MockMediaCapturePlanningLoader implements IMediaCapturePlanningLoader {
    private List<ProgramDescriptorDTO> programs = new ArrayList<>();

    @Override
    public MediaCapturePlanningDTO loadMediaCapturePlanning() {
        return MediaCapturePlanningDTO.builder()
            .programsToCapture(programs)
            .build();
    }

    public void addProgram(String title, String streamURL, long startTimeUTCEpochSec, long durationSeconds, ZoneId timeZone) {
        ProgramId programId = new ProgramId(UUID.randomUUID().toString());
        programs.add(new ProgramDescriptorDTO(programId, title, streamURL, startTimeUTCEpochSec, durationSeconds, timeZone));
    }

    public void clearPrograms() {
        programs.clear();
    }

    public List<ProgramDescriptorDTO> getPrograms() {
        return new ArrayList<>(programs);
    }
}
