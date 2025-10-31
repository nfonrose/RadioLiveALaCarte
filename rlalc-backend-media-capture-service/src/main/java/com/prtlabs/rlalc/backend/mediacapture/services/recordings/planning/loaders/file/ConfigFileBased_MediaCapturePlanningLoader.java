package com.prtlabs.rlalc.backend.mediacapture.services.recordings.planning.loaders.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prtlabs.utils.exceptions.PrtTechnicalRuntimeException;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.planning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.planning.dto.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.exceptions.RLALCExceptionCodesEnum;
import com.prtlabs.utils.json.PrtJsonUtils;

import java.io.File;
import java.io.IOException;

/**
 * Class responsible for reading and parsing the media capture planning configuration.
 * This class uses Jackson to read the JSON content of the grvfm-backend-media-capture-service.conf file.
 */
public class ConfigFileBased_MediaCapturePlanningLoader implements IMediaCapturePlanningLoader {

    private static final String PRTLABS_BASEDIR = System.getProperty("prt.rlalc.baseDir", "/opt/prtlabs");

    private static ObjectMapper mapper = PrtJsonUtils.getFasterXmlObjectMapper();


    /**
     * Loads the media capture planning from the specified configuration file path.
     *
     * @param configFilePath the path to the configuration file to load
     * @param alternateConfigFilePath the path to the alternate configuration file to load if the other one can't be found (used mainly to default to a "dev" config)
     * @throws PrtTechnicalRuntimeException If the configuration can't be read
     * @return the loaded MediaCapturePlanning object
     */
    public MediaCapturePlanningDTO loadMediaCapturePlanning() {
        // Use the configuration file pointed to by 'prt.rlalc.confFileAbsolutePath' or default to the one under PRTLABS_BASEDIR
        String configFilePath = System.getProperty("prt.rlalc.confFileAbsolutePath", (PRTLABS_BASEDIR+"/radiolivealacarte/conf/rlalc-media-capture-batch.conf"));
        return fromFile(new File(configFilePath));
    }


    //
    //
    // IMPLEMENTATION
    //
    //

    /**
     * Loads the media capture planning from the specified configuration file.
     * 
     * @param configFile           the configuration file to load
     * @param alternateConfigFile  the alternate configuration file to load if the other one can't be found (used mainly to default to a "dev" config)
     * @throws PrtTechnicalRuntimeException If the configuration can't be read
     * @return the loaded MediaCapturePlanning object
     */
    private static MediaCapturePlanningDTO fromFile(File configFile) throws PrtTechnicalRuntimeException {
        try {
            return mapper.readValue(configFile, MediaCapturePlanningDTO.class);
        } catch (IOException exConfigFile) {
            // We can't read the configuration from either the normal or the default config file
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_000_FailedToReadConfiguration.name(),
                String.format("Can't read configuration from [%s] with message=[%s]", configFile.getAbsolutePath(), exConfigFile.getMessage()),
                exConfigFile);
        }
    }

}