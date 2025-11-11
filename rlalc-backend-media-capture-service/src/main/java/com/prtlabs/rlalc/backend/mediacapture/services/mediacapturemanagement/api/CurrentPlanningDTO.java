package com.prtlabs.rlalc.backend.mediacapture.services.mediacapturemanagement.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for the current planning and scheduled jobs.
 * Used as the return type for the getCurrentPlanning endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current planning and scheduled jobs information")
public class CurrentPlanningDTO {

    @JsonProperty("loadedPlanning")
    @Schema(description = "List of programs in the current planning")
    private List<ProgramDescriptorDTO> loadedPlanning;

    @JsonProperty("scheduledJobs")
    @Schema(description = "List of currently scheduled Quartz jobs")
    private List<ScheduledJobDTO> scheduledJobs;

    /**
     * DTO for scheduled job information.
     * Used to represent a Quartz job in the scheduledJobs list.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Scheduled job information")
    public static class ScheduledJobDTO {

        @JsonProperty("jobKey")
        @Schema(description = "The name of the job key")
        private String jobKey;

        @JsonProperty("jobGroup")
        @Schema(description = "The group of the job key")
        private String jobGroup;

        @JsonProperty("nextFireTime")
        @Schema(description = "The next fire time of the job (ISO-8601 format)")
        private String nextFireTime;

        @JsonProperty("streamName")
        @Schema(description = "The title of the program")
        private String streamName;

        @JsonProperty("streamURL")
        @Schema(description = "The URL of the stream")
        private String streamURL;

        @JsonProperty("uuid")
        @Schema(description = "The UUID of the program")
        private String uuid;
    }
}
