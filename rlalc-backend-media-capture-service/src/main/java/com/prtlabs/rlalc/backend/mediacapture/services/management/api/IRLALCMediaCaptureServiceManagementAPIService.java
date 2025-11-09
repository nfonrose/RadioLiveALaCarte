package com.prtlabs.rlalc.backend.mediacapture.services.management.api;

import com.prtlabs.rlalc.domain.ProgramDescriptorDTO;
import java.util.List;


public interface IRLALCMediaCaptureServiceManagementAPIService {

    List<ProgramDescriptorDTO> getPlannings();
    void addOneShotTestRecording(ProgramDescriptorDTO programDescriptorDTO);

}
