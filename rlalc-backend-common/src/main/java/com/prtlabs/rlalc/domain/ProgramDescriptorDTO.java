package com.prtlabs.rlalc.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 *    {
 *        "title": "France Inter",
 *        "uuid": "7bf97a79-9612-411d-966b-657b6d77443e",
 *        "streamurl": "http://direct.franceinter.fr/live/franceinter-midfi.mp3",
 *        "startTimeUTCEpochSec": "1761605906",
 *        "durationSeconds": "1200"
 *    }
 *
 */
@Getter
@Builder
public class ProgramDescriptorDTO {

    @JsonProperty("uuid")                 private final String uuid;
    @JsonProperty("title")                private final String title;
    @JsonProperty("streamURL")            private final String streamURL;
    @JsonProperty("startTimeUTCEpochSec") private final long startTimeUTCEpochSec;
    @JsonProperty("durationSeconds")      private final long durationSeconds;

    /**
     * This constructor should be created by Lombok but we need to apply the @TeevityDTO solution
     * TODO - Check what the @TeevityDTO solution is actually
     */
    @JsonCreator
    public ProgramDescriptorDTO(@JsonProperty("uuid") String uuid, @JsonProperty("title") String title, @JsonProperty("streamURL") String streamURL, @JsonProperty("startTimeUTCEpochSec") long startTimeUTCEpochSec, @JsonProperty("durationSeconds") long durationSeconds) {
        this.uuid = uuid;
        this.title = title;
        this.streamURL = streamURL;
        this.startTimeUTCEpochSec = startTimeUTCEpochSec;
        this.durationSeconds = durationSeconds;
    }

}
