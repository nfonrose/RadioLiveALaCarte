package com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.loaders.codedefined;

import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.dto.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.dto.StreamToCaptureDTO;
import com.prtlabs.rlalc.domain.ProgramDescriptor;
import lombok.AllArgsConstructor;

import java.util.*;

public class StaticallyDefined_MediaCapturePlanningLoader implements IMediaCapturePlanningLoader {

    private String title;
    private String streamURL;
    private long startTimeUTCEpochSec;
    private long durationSeconds;
    private List<ProgramDescriptor> planningContent = new ArrayList<ProgramDescriptor>();

    public StaticallyDefined_MediaCapturePlanningLoader(String title, String streamURL, long startTimeUTCEpochSec, long durationSeconds) {
        this.planningContent = Arrays.asList(new ProgramDescriptor(null, title, streamURL, startTimeUTCEpochSec, durationSeconds, Optional.empty()));
        this.title = title;
        this.streamURL = streamURL;
        this.startTimeUTCEpochSec = startTimeUTCEpochSec;
        this.durationSeconds = durationSeconds;
    }

    @Override
    public MediaCapturePlanningDTO loadMediaCapturePlanning() {
        // Build a MediaCapturePlanning
        return MediaCapturePlanningDTO.builder()
            .streamsToCapture(Arrays.asList(StreamToCaptureDTO.builder()
                .title(this.title)
                .uuid(UUID.randomUUID().toString())
                .streamurl(this.streamURL)
                .startTimeUTCEpochSec(this.startTimeUTCEpochSec)
                .durationSeconds(this.durationSeconds)
                .build()))
            .build();
    }
}
