package com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stream to capture class.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamToCaptureDTO {
    private String title;
    private String uuid;
    private String streamurl;
    private String startTimeUTCEpochSec;
    private String durationSeconds;
}
