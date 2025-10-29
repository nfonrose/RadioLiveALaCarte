package com.prtlabs.rlalc.backend.mediacapture.services.mediacaptureplanning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Stream to capture class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamToCaptureDTO {
    private String title;
    private String uuid;
    private String streamurl;
    private String startTimeUTCEpochSec;
    private String durationSeconds;

    /**
     * Gets the title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the UUID.
     *
     * @return the UUID
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets the UUID.
     *
     * @param uuid the UUID to set
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Gets the stream URL.
     *
     * @return the stream URL
     */
    public String getStreamurl() {
        return streamurl;
    }

    /**
     * Sets the stream URL.
     *
     * @param streamurl the stream URL to set
     */
    public void setStreamurl(String streamurl) {
        this.streamurl = streamurl;
    }

    /**
     * Gets the start time in UTC epoch seconds.
     *
     * @return the start time in UTC epoch seconds
     */
    public String getStartTimeUTCEpochSec() {
        return startTimeUTCEpochSec;
    }

    /**
     * Sets the start time in UTC epoch seconds.
     *
     * @param startTimeUTCEpochSec the start time in UTC epoch seconds to set
     */
    public void setStartTimeUTCEpochSec(String startTimeUTCEpochSec) {
        this.startTimeUTCEpochSec = startTimeUTCEpochSec;
    }

    /**
     * Gets the duration in seconds.
     *
     * @return the duration in seconds
     */
    public String getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Sets the duration in seconds.
     *
     * @param durationSeconds the duration in seconds to set
     */
    public void setDurationSeconds(String durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
