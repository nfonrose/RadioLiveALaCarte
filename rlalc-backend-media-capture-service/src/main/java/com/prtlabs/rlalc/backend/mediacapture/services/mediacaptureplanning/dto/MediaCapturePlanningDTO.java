package com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Class responsible for reading and parsing the media capture planning configuration.
 * This class uses Jackson to read the JSON content of the grvfm-backend-media-capture-service.conf file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaCapturePlanningDTO {

    private Meta meta;

    @JsonProperty("streamsToCapture")
    private List<StreamToCaptureDTO> streamsToCapture;

    /**
     * Meta information class.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        private String format;
    }

}
