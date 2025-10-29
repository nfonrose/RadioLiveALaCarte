package com.prtlabs.rlalc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
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
@Builder
@AllArgsConstructor
public class ProgramDescriptorDTO {

    private String uuid;
    private String title;
    private String streamURL;
    private long startTimeUTCEpochSec;
    private long durationSeconds;

}
