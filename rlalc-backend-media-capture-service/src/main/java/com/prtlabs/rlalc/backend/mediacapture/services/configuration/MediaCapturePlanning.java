package com.prtlabs.rlalc.backend.mediacapture.services.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prtlabs.exceptions.PrtTechnicalRuntimeException;
import com.prtlabs.rlalc.exceptions.RLALCExceptionCodesEnum;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Class responsible for reading and parsing the media capture planning configuration.
 * This class uses Jackson to read the JSON content of the grvfm-backend-media-capture-service.conf file.
 */
public class MediaCapturePlanning {

    private Meta meta;

    @JsonProperty("streamsToCapture")
    private List<StreamToCapture> streamsToCapture;

    /**
     * Default constructor.
     */
    public MediaCapturePlanning() {
    }

    /**
     * Loads the media capture planning from the specified configuration file.
     * 
     * @param configFile           the configuration file to load
     * @param alternateConfigFile  the alternate configuration file to load if the other one can't be found (used mainly to default to a "dev" config)
     * @throws PrtTechnicalRuntimeException If the configuration can't be read
     * @return the loaded MediaCapturePlanning object
     */
    public static MediaCapturePlanning fromFile(File configFile, File alternateConfigFile) throws PrtTechnicalRuntimeException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(configFile, MediaCapturePlanning.class);
        } catch (IOException exConfigFile) {
            try {
                return mapper.readValue(alternateConfigFile, MediaCapturePlanning.class);
            } catch (IOException exDefaultConfigFile) {
                // We can't read the configuration from either the normal or the default config file
                throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_000_FailedToReadConfiguration.name(),
                    String.format("Can't read configuration either from [%s] with message=[%s] or from [%s] with message=[%s]",
                        configFile.getAbsolutePath(), exConfigFile.getMessage(),
                        alternateConfigFile.getAbsolutePath(), exDefaultConfigFile.getMessage()
                    ),
                    exDefaultConfigFile);
            }
        }
    }

    /**
     * Loads the media capture planning from the specified configuration file path.
     * 
     * @param configFilePath the path to the configuration file to load
     * @param alternateConfigFilePath the path to the alternate configuration file to load if the other one can't be found (used mainly to default to a "dev" config)
     * @throws PrtTechnicalRuntimeException If the configuration can't be read
     * @return the loaded MediaCapturePlanning object
     */
    public static MediaCapturePlanning fromFile(String configFilePath, String alternateConfigFilePath) {
        return fromFile(new File(configFilePath), new File(alternateConfigFilePath));
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
    public List<StreamToCapture> getStreamsToCapture() {
        return streamsToCapture;
    }

    /**
     * Sets the list of streams to capture.
     * 
     * @param streamsToCapture the list of streams to capture to set
     */
    public void setStreamsToCapture(List<StreamToCapture> streamsToCapture) {
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

    /**
     * Stream to capture class.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StreamToCapture {
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
}