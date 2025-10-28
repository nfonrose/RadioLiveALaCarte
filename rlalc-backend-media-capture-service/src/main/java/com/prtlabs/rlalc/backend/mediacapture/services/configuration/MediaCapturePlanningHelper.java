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
public class MediaCapturePlanningHelper {

    /**
     * Loads the media capture planning from the specified configuration file.
     * 
     * @param configFile           the configuration file to load
     * @param alternateConfigFile  the alternate configuration file to load if the other one can't be found (used mainly to default to a "dev" config)
     * @throws PrtTechnicalRuntimeException If the configuration can't be read
     * @return the loaded MediaCapturePlanning object
     */
    public static MediaCapturePlanningDTO fromFile(File configFile, File alternateConfigFile) throws PrtTechnicalRuntimeException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(configFile, MediaCapturePlanningDTO.class);
        } catch (IOException exConfigFile) {
            try {
                return mapper.readValue(alternateConfigFile, MediaCapturePlanningDTO.class);
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
    public static MediaCapturePlanningDTO fromFile(String configFilePath, String alternateConfigFilePath) {
        return fromFile(new File(configFilePath), new File(alternateConfigFilePath));
    }

}