package com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.loaders.codedefined;

import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.dto.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.domain.ProgramId;

import java.time.ZoneId;
import java.util.*;

public class StaticallyDefined_MediaCapturePlanningLoader implements IMediaCapturePlanningLoader {

    private List<ProgramDescriptorDTO> planningContent = new ArrayList<ProgramDescriptorDTO>();

    public StaticallyDefined_MediaCapturePlanningLoader(String title, String streamURL, long startTimeUTCEpochSec, long durationSeconds, ZoneId timeZone) {
        this.planningContent = Arrays.asList(new ProgramDescriptorDTO(null, title, streamURL, startTimeUTCEpochSec, durationSeconds, timeZone));
    }

    public StaticallyDefined_MediaCapturePlanningLoader(List<ProgramDescriptorDTO> planningContent) {
        this.planningContent = new ArrayList<>(planningContent);
    }

    @Override
    public MediaCapturePlanningDTO loadMediaCapturePlanning() {
        // Build a MediaCapturePlanning
        return MediaCapturePlanningDTO.builder()
            .programsToCapture(planningContent)
            .build();
    }
}
