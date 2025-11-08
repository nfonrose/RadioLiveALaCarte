package com.prtlabs.rlalc.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.swing.text.html.Option;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
@SuperBuilder(toBuilder=true)
public class ProgramDescriptorDTO {

    @JsonProperty("uuid")                       private final ProgramId uuid;
    @JsonProperty("title")                      private final String title;
    @JsonProperty("streamURL")                  private final String streamURL;
    @JsonProperty("startTimeUTCEpochSec")       private final long startTimeUTCEpochSec;
    @JsonProperty("durationSeconds")            private final long durationSeconds;
    @JsonProperty("timeZone")                   private final ZoneId timeZone;
    @JsonProperty("recorderSpecificParameters") private final Map<String, String> recorderSpecificParameters;

    /**
     * This constructor should be created by Lombok but we need to apply the @TeevityDTO solution
     * TODO - Check what the @TeevityDTO solution is actually
     */
    @JsonCreator
    public ProgramDescriptorDTO(@JsonProperty("uuid") ProgramId uuid, @JsonProperty("title") String title, @JsonProperty("streamURL") String streamURL, @JsonProperty("startTimeUTCEpochSec") long startTimeUTCEpochSec, @JsonProperty("durationSeconds") long durationSeconds, @JsonProperty("timeZone") ZoneId timeZone, @JsonProperty("recorderSpecificParameters") Map<String, String> recorderSpecificParameters) {
        this.uuid = uuid;
        this.title = title;
        this.streamURL = streamURL;
        this.startTimeUTCEpochSec = startTimeUTCEpochSec;
        this.durationSeconds = durationSeconds;
        this.timeZone = timeZone;
        this.recorderSpecificParameters = recorderSpecificParameters;
    }

    public ProgramDescriptorDTO(@JsonProperty("uuid") ProgramId uuid, @JsonProperty("title") String title, @JsonProperty("streamURL") String streamURL, @JsonProperty("startTimeUTCEpochSec") long startTimeUTCEpochSec, @JsonProperty("durationSeconds") long durationSeconds, @JsonProperty("timeZone") ZoneId timeZone) {
        this(uuid, title, streamURL, startTimeUTCEpochSec, durationSeconds, timeZone, new HashMap<>());
    }

}
