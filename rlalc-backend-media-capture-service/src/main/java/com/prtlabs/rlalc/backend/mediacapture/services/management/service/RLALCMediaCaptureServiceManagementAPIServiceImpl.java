package com.prtlabs.rlalc.backend.mediacapture.services.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.prtlabs.rlalc.backend.mediacapture.services.management.api.CurrentPlanningDTO;
import com.prtlabs.rlalc.backend.mediacapture.services.management.api.IRLALCMediaCaptureServiceManagementAPIService;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.scheduling.quartzjobs.MediaCaptureJob;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.planning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.recordings.planning.dto.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.domain.ProgramId;
import com.prtlabs.rlalc.exceptions.RLALCExceptionCodesEnum;
import com.prtlabs.utils.exceptions.PrtTechnicalRuntimeException;
import com.prtlabs.utils.json.PrtJsonUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.text.SimpleDateFormat;
import java.util.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;


@Path("management")
@Tag(name = "management")
public class RLALCMediaCaptureServiceManagementAPIServiceImpl implements IRLALCMediaCaptureServiceManagementAPIService {

    @Inject
    private IMediaCapturePlanningLoader mediaCapturePlanningLoader;

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

    /**
     * Can be called with:
     *   curl -s http://localhost:9796/api/management/planning/current -H "Accept: application/json" | jq .
     */
    @GET
    @Path("/planning/current")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get the current planning and scheduled jobs",
        description = "Returns the current loaded planning and the currently scheduled Quartz jobs"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved current planning and scheduled jobs",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CurrentPlanningDTO.class))
    )
    @ApiResponse(
        responseCode = "500",
        description = "Technical error occurred while retrieving scheduler information"
    )
    @Override
    public CurrentPlanningDTO getCurrentPlanning() {
        try {
            // Get the loaded planning from the planning loader
            MediaCapturePlanningDTO planning = mediaCapturePlanningLoader.loadMediaCapturePlanning();
            List<ProgramDescriptorDTO> loadedPlanning = planning.getProgramsToCapture();

            // Get the scheduled jobs from the Quartz scheduler
            List<CurrentPlanningDTO.ScheduledJobDTO> scheduledJobs = new ArrayList<>();

            // Initialize the scheduler
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();

            // Get all job keys
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.anyGroup())) {
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

                if (!triggers.isEmpty()) {
                    Trigger trigger = triggers.get(0);
                    Date nextFireTime = trigger.getNextFireTime();

                    // Create a builder for the ScheduledJobDTO
                    CurrentPlanningDTO.ScheduledJobDTO.ScheduledJobDTOBuilder jobBuilder = CurrentPlanningDTO.ScheduledJobDTO.builder()
                        .jobKey(jobKey.getName())
                        .jobGroup(jobKey.getGroup())
                        .nextFireTime(nextFireTime != null ? 
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(nextFireTime) : null);

                    // Try to extract stream name from job data
                    JobDataMap jobDataMap = jobDetail.getJobDataMap();
                    if (jobDataMap.containsKey(MediaCaptureJob.KEY_PROGRAM_DESC_ASJSON)) {
                        try {
                            ProgramDescriptorDTO programDescriptor = PrtJsonUtils.getFasterXmlObjectMapper()
                                .readValue(jobDataMap.getString(MediaCaptureJob.KEY_PROGRAM_DESC_ASJSON), ProgramDescriptorDTO.class);
                            jobBuilder
                                .streamName(programDescriptor.getTitle())
                                .streamURL(programDescriptor.getStreamURL())
                                .uuid(programDescriptor.getUuid() != null ? 
                                    programDescriptor.getUuid().toString() : null);
                        } catch (Exception e) {
                            // If we can't parse the program descriptor, just continue without this info
                            jobBuilder.streamName("Unknown");
                        }
                    }

                    scheduledJobs.add(jobBuilder.build());
                }
            }

            // Create and return the DTO
            return CurrentPlanningDTO.builder()
                .loadedPlanning(loadedPlanning)
                .scheduledJobs(scheduledJobs)
                .build();

        } catch (SchedulerException e) {
            throw new PrtTechnicalRuntimeException(
                RLALCExceptionCodesEnum.RLAC_001_FailedToScheduleMediaCapture.name(), 
                "Failed to access Quartz scheduler: " + e.getMessage(), 
                e
            );
        }
    }

}
