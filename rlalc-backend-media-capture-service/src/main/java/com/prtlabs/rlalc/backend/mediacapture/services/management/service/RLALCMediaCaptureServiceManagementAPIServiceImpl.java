package com.prtlabs.rlalc.backend.mediacapture.services.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prtlabs.rlalc.backend.mediacapture.services.management.api.IRLALCMediaCaptureServiceManagementAPIService;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.domain.ProgramId;
import com.prtlabs.rlalc.exceptions.RLALCExceptionCodesEnum;
import com.prtlabs.utils.exceptions.PrtTechnicalRuntimeException;
import com.prtlabs.utils.json.PrtJsonUtils;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


@Path("management")
@Tag(name = "management")
public class RLALCMediaCaptureServiceManagementAPIServiceImpl implements IRLALCMediaCaptureServiceManagementAPIService {

    /**
     * Can be called with:
     *   curl -s http://localhost:9796/api/management/recordingsPlanning -H "Accept: application/json" | jq .
     */
    @GET
    @Operation(summary = "Get the recordings planning", description = "Return the planning for the recordings, including the one added on the fly via the API")
    @Path("/recordingsPlanning")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public List<ProgramDescriptorDTO> getPlannings() {
        return List.of(
            ProgramDescriptorDTO.builder().build(),
            ProgramDescriptorDTO.builder().build()
        );
    }

    /**
     * Add a test recording that will run 1 sec after the call for 15sec (unless another duration is specified, ie is >0 sec)
     * Can be called with:
     *   curl -s http://localhost:9796/api/management/addOneShotTestRecording -X POST -H "Content-Type: application/json" -d '{"uuid":null,"title":"France Inter - Test recording","streamURL":null,"startTimeUTCEpochSec":0,"durationSeconds":0,"timeZone":null,"recorderSpecificParameters":null}'
     */
    @POST
    @Path("/addOneShotTestRecording")
    @Consumes(MediaType.APPLICATION_JSON)
    @Override
    public void addOneShotTestRecording(ProgramDescriptorDTO programDescriptorDTO) {
        // Assign a new UUID to the recording
        String uuid = UUID.randomUUID().toString();
        programDescriptorDTO = programDescriptorDTO.toBuilder()
            .uuid(new ProgramId(uuid))
            .durationSeconds( ((programDescriptorDTO.getDurationSeconds()>0) ? programDescriptorDTO.getDurationSeconds() : 15) )
            .build();
        // Dump the content of the updated programDescriptorDTO to stdout
        try {
            System.out.println(PrtJsonUtils.getFasterXmlObjectMapper().writeValueAsString(programDescriptorDTO));
        } catch (JsonProcessingException e) {
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_010_CannotAddOnTheFlyOneShotTestRecording.name(), e.getMessage(), e);
        }
    }

}
