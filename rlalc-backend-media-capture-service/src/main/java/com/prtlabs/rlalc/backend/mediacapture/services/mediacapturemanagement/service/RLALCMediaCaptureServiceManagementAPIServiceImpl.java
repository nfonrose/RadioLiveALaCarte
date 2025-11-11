package com.prtlabs.rlalc.backend.mediacapture.services.mediacapturemanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturemanagement.api.CurrentPlanningDTO;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturemanagement.api.IRLALCMediaCaptureServiceManagementAPIService;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.IRLALCMediaCaptureService;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.scheduling.quartzjobs.MediaCaptureJob;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.planning.IMediaCapturePlanningLoader;
import com.prtlabs.rlalc.backend.mediacapture.services.mediacapturebatch.recordings.planning.dto.MediaCapturePlanningDTO;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import com.prtlabs.rlalc.domain.ProgramId;
import com.prtlabs.rlalc.exceptions.RLALCExceptionCodesEnum;
import com.prtlabs.utils.exceptions.PrtTechnicalRuntimeException;
import com.prtlabs.utils.json.PrtJsonUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


@Path("management")
@Tag(name = "management")
public class RLALCMediaCaptureServiceManagementAPIServiceImpl implements IRLALCMediaCaptureServiceManagementAPIService {

    private static final Logger logger = LoggerFactory.getLogger(RLALCMediaCaptureServiceManagementAPIServiceImpl.class);

    @Inject
    private IMediaCapturePlanningLoader mediaCapturePlanningLoader;

    @Inject
    private IRLALCMediaCaptureService mediaCaptureService;

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
     *   curl -s http://localhost:9796/api/management/addOneShotTestRecording -X POST -H "Content-Type: application/json" -d '{"title":"France Inter - Test recording","streamURL":"http://direct.franceinter.fr/live/franceinter-midfi.mp3","timeZone":"Europe/Paris", "recorderSpecificParameters":{}}'
     */
    @POST
    @Path("/addOneShotTestRecording")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Add a one-shot test recording",
        description = "Adds a test recording that will run 1 second after the call for 15 seconds (unless another duration is specified)"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Successfully scheduled the test recording"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Technical error occurred while scheduling the test recording"
    )
    @Override
    public void addOneShotTestRecording(ProgramDescriptorDTO programDescriptorDTO) {
        try {
            // Preconditions
            checkArgument(!Strings.isNullOrEmpty(programDescriptorDTO.getTitle()), "The title must be defined");
            checkNotNull(programDescriptorDTO.getTimeZone(), "The timeZone must be defined for testRecording.title=[%s]", programDescriptorDTO.getTitle());
            checkNotNull(programDescriptorDTO.getRecorderSpecificParameters(), "The recorderSpecificParameters can't be null (it should be an empty map instead) for testRecording.title=[%s]", programDescriptorDTO.getTitle());

            // Build the updated program descriptor
            String uuid = UUID.randomUUID().toString();
            long startTimeEpochSec = Instant.now().plusSeconds(1).getEpochSecond();    // Set start time to 1 second from now
            programDescriptorDTO = programDescriptorDTO.toBuilder()
                .uuid(new ProgramId(uuid))
                .startTimeUTCEpochSec(startTimeEpochSec)
                .durationSeconds(programDescriptorDTO.getDurationSeconds() > 0 ? programDescriptorDTO.getDurationSeconds() : 15)
                .build();

            // Schedule the one-time media capture
            logger.info("Adding one-shot test recording [{}]", PrtJsonUtils.getFasterXmlObjectMapper().writeValueAsString(programDescriptorDTO));
            ProgramId programId = mediaCaptureService.addOneTimeMediaCapture(programDescriptorDTO);
            logger.info("  -> Successfully scheduled one-shot test recording with ID [{}]", programId);
        } catch (JsonProcessingException e) {
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_010_CannotAddOnTheFlyOneShotTestRecording.name(), "Failed to process program descriptor with message=["+e.getMessage()+"]", e);
        } catch (Exception e) {throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_010_CannotAddOnTheFlyOneShotTestRecording.name(), "Failed to add one-shot test recording: "+e.getMessage()+"]", e);
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
