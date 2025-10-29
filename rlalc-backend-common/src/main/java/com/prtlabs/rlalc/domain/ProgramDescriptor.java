package com.prtlabs.rlalc.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Optional;

@Getter
@AllArgsConstructor
public class ProgramDescriptor {

    private String uuid;
    private String streamURL;
    private String name;
    private long startTimeUTCEpochSec;
    private long durationSeconds;

    public Optional<String> chunkFileNamePrefix;

}
