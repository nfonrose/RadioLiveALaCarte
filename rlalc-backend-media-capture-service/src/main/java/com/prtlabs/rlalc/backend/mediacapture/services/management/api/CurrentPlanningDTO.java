package com.prtlabs.rlalc.backend.mediacapture.services.management.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for the current planning and scheduled jobs.
 * Used as the return type for the getCurrentPlanning endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentPlanningDTO {

    @JsonProperty("loadedPlanning")
    private List<ProgramDescriptorDTO> loadedPlanning;

    @JsonProperty("scheduledJobs")
    private List<Map<String, Object>> scheduledJobs;
}