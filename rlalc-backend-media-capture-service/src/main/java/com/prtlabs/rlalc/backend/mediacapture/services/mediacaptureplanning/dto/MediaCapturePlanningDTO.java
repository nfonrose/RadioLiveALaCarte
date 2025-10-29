package com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Class responsible for reading and parsing the media capture planning configuration.
 * This class uses Jackson to read the JSON content of the grvfm-backend-media-capture-service.conf file.
 */
public class MediaCapturePlanningDTO {

    private Meta meta;

    @JsonProperty("streamsToCapture")
    private List<StreamToCaptureDTO> streamsToCapture;

    /**
     * Default constructor.
     */
    public MediaCapturePlanningDTO() {
    }
    /**
     * Gets the meta information.
     * 
     * @return the meta information
     */
    public Meta getMeta() {
        return meta;
    }

    /**
     * Sets the meta information.
     * 
     * @param meta the meta information to set
     */
    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    /**
     * Gets the list of streams to capture.
     * 
     * @return the list of streams to capture
     */
    public List<StreamToCaptureDTO> getStreamsToCapture() {
        return streamsToCapture;
    }

    /**
     * Sets the list of streams to capture.
     * 
     * @param streamsToCapture the list of streams to capture to set
     */
    public void setStreamsToCapture(List<StreamToCaptureDTO> streamsToCapture) {
        this.streamsToCapture = streamsToCapture;
    }

    /**
     * Meta information class.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        private String format;

        /**
         * Gets the format.
         * 
         * @return the format
         */
        public String getFormat() {
            return format;
        }

        /**
         * Sets the format.
         * 
         * @param format the format to set
         */
        public void setFormat(String format) {
            this.format = format;
        }
    }

}